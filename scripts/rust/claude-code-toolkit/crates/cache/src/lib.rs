//! `cct-cache` — Content-addressed cache layer for H-Guard scanner results.
//!
//! Architecture:
//! - [`hasher`]: blake3::Hasher streams method body bytes into 32-byte hashes.
//! - [`store`]: redb ACID database with tables for (hash → ScanResult) and method metadata.
//! - [`manager`]: CacheManager wraps the store with a thread-safe query/insert API.
//!
//! All hashes are blake3 256-bit digests; serialization uses bincode for compact storage.

// ── Hasher Module ─────────────────────────────────────────────────────────────

/// Content-addressed hash generation using blake3.
pub mod hasher {
    use blake3::Hasher;

    /// Hash method body bytes into a 32-byte blake3 digest.
    ///
    /// Uses blake3::Hasher for streaming support (suitable for large files).
    pub fn hash_method_body(body_bytes: &[u8]) -> [u8; 32] {
        let mut hasher = Hasher::new();
        hasher.update(body_bytes);
        let digest = hasher.finalize();
        let mut hash = [0u8; 32];
        hash.copy_from_slice(digest.as_bytes());
        hash
    }

    #[cfg(test)]
    mod tests {
        use super::*;

        #[test]
        fn test_hash_method_body_deterministic() {
            let body = b"public void foo() { return null; }";
            let hash1 = hash_method_body(body);
            let hash2 = hash_method_body(body);
            assert_eq!(hash1, hash2, "same input should produce same hash");
        }

        #[test]
        fn test_hash_method_body_different_inputs() {
            let body1 = b"public void foo() { return null; }";
            let body2 = b"public void bar() { return null; }";
            let hash1 = hash_method_body(body1);
            let hash2 = hash_method_body(body2);
            assert_ne!(hash1, hash2, "different inputs should produce different hashes");
        }

        #[test]
        fn test_hash_method_body_returns_32_bytes() {
            let body = b"test";
            let hash = hash_method_body(body);
            assert_eq!(hash.len(), 32, "blake3 digest must be 32 bytes");
        }

        #[test]
        fn test_hash_streaming_equivalence() {
            let part1 = b"public void ";
            let part2 = b"foo() { return null; }";

            let mut combined = part1.to_vec();
            combined.extend_from_slice(part2);
            let hash_direct = hash_method_body(&combined);

            let hash_streamed = {
                let mut hasher = Hasher::new();
                hasher.update(part1);
                hasher.update(part2);
                let digest = hasher.finalize();
                let mut hash = [0u8; 32];
                hash.copy_from_slice(digest.as_bytes());
                hash
            };
            assert_eq!(
                hash_direct, hash_streamed,
                "streaming hashing should match direct hashing"
            );
        }
    }
}

// ── Store Module ──────────────────────────────────────────────────────────────

/// ACID database store using redb for (hash → ScanResult) and metadata tables.
pub mod store {
    use anyhow::Result;
    use redb::{Database, ReadableTable, TableDefinition};
    use serde::{Deserialize, Serialize};
    use std::path::Path;

    /// Represents a cached scan result stored in the database.
    #[derive(Debug, Clone, Serialize, Deserialize)]
    pub struct CachedScanResult {
        /// Path of the source file.
        pub path: String,
        /// Number of violations found.
        pub violation_count: usize,
        /// Timestamp when cached (seconds since UNIX_EPOCH).
        pub cached_at: u64,
    }

    /// Table definition: hash (32 bytes) → bincode-serialized CachedScanResult.
    const SCAN_RESULTS_TABLE: TableDefinition<&[u8; 32], &[u8]> =
        TableDefinition::new("scan_results");

    /// Table definition: hash (32 bytes) → method metadata (method name string).
    const METHOD_METADATA_TABLE: TableDefinition<&[u8; 32], &str> =
        TableDefinition::new("method_metadata");

    /// ACID cache store backed by redb.
    pub struct Store {
        db: Database,
    }

    impl Store {
        /// Open or create a cache database at `db_path`.
        pub fn new(db_path: &Path) -> Result<Self> {
            let db = Database::create(db_path)?;
            // Initialize tables on creation
            {
                let write_txn = db.begin_write()?;
                let _ = write_txn.open_table(SCAN_RESULTS_TABLE);
                let _ = write_txn.open_table(METHOD_METADATA_TABLE);
                write_txn.commit()?;
            }
            Ok(Store { db })
        }

        /// Insert a scan result for a given hash.
        pub fn insert(&self, hash: &[u8; 32], result: &CachedScanResult) -> Result<()> {
            let write_txn = self.db.begin_write()?;
            {
                let mut table = write_txn.open_table(SCAN_RESULTS_TABLE)?;
                let encoded = bincode::serialize(result)?;
                table.insert(hash, encoded.as_slice())?;
            }
            write_txn.commit()?;
            Ok(())
        }

        /// Query a scan result by hash; returns None if not found.
        pub fn query(&self, hash: &[u8; 32]) -> Result<Option<CachedScanResult>> {
            let read_txn = self.db.begin_read()?;
            let table = read_txn.open_table(SCAN_RESULTS_TABLE)?;
            match table.get(hash)? {
                Some(entry) => {
                    let bytes = entry.value().to_vec();
                    let result = bincode::deserialize(&bytes)?;
                    Ok(Some(result))
                }
                None => Ok(None),
            }
        }

        /// Store method name metadata for a hash.
        pub fn insert_method_metadata(&self, hash: &[u8; 32], method_name: &str) -> Result<()> {
            let write_txn = self.db.begin_write()?;
            {
                let mut table = write_txn.open_table(METHOD_METADATA_TABLE)?;
                table.insert(hash, method_name)?;
            }
            write_txn.commit()?;
            Ok(())
        }

        /// Retrieve method name metadata by hash.
        pub fn query_method_metadata(&self, hash: &[u8; 32]) -> Result<Option<String>> {
            let read_txn = self.db.begin_read()?;
            let table = read_txn.open_table(METHOD_METADATA_TABLE)?;
            match table.get(hash)? {
                Some(entry) => Ok(Some(entry.value().to_owned())),
                None => Ok(None),
            }
        }

        /// Check if a hash exists in the cache.
        pub fn contains(&self, hash: &[u8; 32]) -> Result<bool> {
            let read_txn = self.db.begin_read()?;
            let table = read_txn.open_table(SCAN_RESULTS_TABLE)?;
            Ok(table.get(hash)?.is_some())
        }

        /// Get the number of cached results (for testing/stats).
        pub fn count(&self) -> Result<usize> {
            let read_txn = self.db.begin_read()?;
            let table = read_txn.open_table(SCAN_RESULTS_TABLE)?;
            Ok(table.iter()?.count())
        }
    }

    #[cfg(test)]
    mod tests {
        use super::*;
        use std::time::{SystemTime, UNIX_EPOCH};

        fn test_result(path: &str, count: usize) -> CachedScanResult {
            let cached_at = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs();
            CachedScanResult {
                path: path.to_string(),
                violation_count: count,
                cached_at,
            }
        }

        #[test]
        fn test_store_roundtrip() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let store = Store::new(&db_path).unwrap();

            let hash = [1u8; 32];
            let result = test_result("test.java", 3);

            store.insert(&hash, &result).unwrap();
            let retrieved = store.query(&hash).unwrap();
            assert!(retrieved.is_some());
            assert_eq!(retrieved.unwrap().path, "test.java");
        }

        #[test]
        fn test_store_cache_hit() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let store = Store::new(&db_path).unwrap();

            let hash = [2u8; 32];
            let result = test_result("Foo.java", 1);
            store.insert(&hash, &result).unwrap();

            let hit = store.contains(&hash).unwrap();
            assert!(hit, "hash should exist in cache");
        }

        #[test]
        fn test_store_cache_miss() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let store = Store::new(&db_path).unwrap();

            let missing_hash = [99u8; 32];
            let miss = store.contains(&missing_hash).unwrap();
            assert!(!miss, "missing hash should not exist");
        }

        #[test]
        fn test_store_method_metadata() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let store = Store::new(&db_path).unwrap();

            let hash = [3u8; 32];
            store.insert_method_metadata(&hash, "processData").unwrap();

            let metadata = store.query_method_metadata(&hash).unwrap();
            assert_eq!(metadata, Some("processData".to_string()));
        }

        #[test]
        fn test_store_multiple_entries() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let store = Store::new(&db_path).unwrap();

            let hash1 = [10u8; 32];
            let hash2 = [20u8; 32];
            let result1 = test_result("File1.java", 0);
            let result2 = test_result("File2.java", 5);

            store.insert(&hash1, &result1).unwrap();
            store.insert(&hash2, &result2).unwrap();

            assert_eq!(store.count().unwrap(), 2, "should have 2 entries");
            assert!(store.query(&hash1).unwrap().is_some());
            assert!(store.query(&hash2).unwrap().is_some());
        }

        #[test]
        fn test_store_hash_collision_handling() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let store = Store::new(&db_path).unwrap();

            let hash = [5u8; 32];
            let result1 = test_result("Original.java", 0);
            let result2 = test_result("Updated.java", 1);

            store.insert(&hash, &result1).unwrap();
            store.insert(&hash, &result2).unwrap();

            let final_result = store.query(&hash).unwrap().unwrap();
            assert_eq!(
                final_result.path, "Updated.java",
                "second insert should overwrite first"
            );
            assert_eq!(final_result.violation_count, 1);
        }
    }
}

// ── Manager Module ────────────────────────────────────────────────────────────

/// Thread-safe cache manager combining hasher and store.
pub mod manager {
    use super::hasher::hash_method_body;
    use super::store::{CachedScanResult, Store};
    use anyhow::Result;
    use std::path::Path;
    use std::sync::Arc;

    /// Cache manager wrapping redb store with blake3 hashing.
    ///
    /// This is `Send + Sync` for safe sharing across rayon threads.
    pub struct CacheManager {
        store: Arc<Store>,
    }

    impl CacheManager {
        /// Create a new cache manager with a database at `db_path`.
        pub fn new(db_path: &Path) -> Result<Self> {
            let store = Store::new(db_path)?;
            Ok(CacheManager {
                store: Arc::new(store),
            })
        }

        /// Hash a method body and query the cache.
        pub fn query(&self, method_body: &[u8]) -> Result<Option<CachedScanResult>> {
            let hash = hash_method_body(method_body);
            self.store.query(&hash)
        }

        /// Hash a method body and insert a result into the cache.
        pub fn insert(&self, method_body: &[u8], result: &CachedScanResult) -> Result<()> {
            let hash = hash_method_body(method_body);
            self.store.insert(&hash, result)
        }

        /// Insert method metadata by pre-computed hash.
        pub fn insert_method_metadata(&self, hash: &[u8; 32], method_name: &str) -> Result<()> {
            self.store.insert_method_metadata(hash, method_name)
        }

        /// Query method metadata by pre-computed hash.
        pub fn query_method_metadata(&self, hash: &[u8; 32]) -> Result<Option<String>> {
            self.store.query_method_metadata(hash)
        }

        /// Check if a method body is in the cache.
        pub fn contains(&self, method_body: &[u8]) -> Result<bool> {
            let hash = hash_method_body(method_body);
            self.store.contains(&hash)
        }

        /// Get count of cached entries (for testing/stats).
        pub fn count(&self) -> Result<usize> {
            self.store.count()
        }
    }

    // Explicitly declare Send + Sync for documentation.
    // Arc<Store> is Send + Sync because Store (Database handle) is Send + Sync.
    unsafe impl Send for CacheManager {}
    unsafe impl Sync for CacheManager {}

    #[cfg(test)]
    mod tests {
        use super::*;
        use std::time::{SystemTime, UNIX_EPOCH};

        fn test_result(path: &str, count: usize) -> CachedScanResult {
            let cached_at = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs();
            CachedScanResult {
                path: path.to_string(),
                violation_count: count,
                cached_at,
            }
        }

        #[test]
        fn test_manager_insert_and_query() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let manager = CacheManager::new(&db_path).unwrap();

            let body = b"public void foo() { return null; }";
            let result = test_result("Test.java", 2);

            manager.insert(body, &result).unwrap();
            let retrieved = manager.query(body).unwrap();
            assert!(retrieved.is_some());
            assert_eq!(retrieved.unwrap().path, "Test.java");
        }

        #[test]
        fn test_manager_cache_hit() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let manager = CacheManager::new(&db_path).unwrap();

            let body = b"public String getName() { return name; }";
            let result = test_result("User.java", 0);

            manager.insert(body, &result).unwrap();
            let hit = manager.contains(body).unwrap();
            assert!(hit, "identical body should be found in cache");
        }

        #[test]
        fn test_manager_cache_miss() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let manager = CacheManager::new(&db_path).unwrap();

            let body = b"never inserted";
            let miss = manager.contains(body).unwrap();
            assert!(!miss, "body not inserted should not be in cache");
        }

        #[test]
        fn test_manager_content_addressing() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let manager = CacheManager::new(&db_path).unwrap();

            let body1 = b"public void a() { }";
            let body2 = b"public void b() { }";

            let result1 = test_result("File1.java", 1);
            let result2 = test_result("File2.java", 0);

            manager.insert(body1, &result1).unwrap();
            manager.insert(body2, &result2).unwrap();

            // Different bodies should have different cache entries
            assert!(manager.query(body1).unwrap().is_some());
            assert!(manager.query(body2).unwrap().is_some());
            let r1 = manager.query(body1).unwrap().unwrap();
            let r2 = manager.query(body2).unwrap().unwrap();
            assert_ne!(r1.path, r2.path);
        }

        #[test]
        fn test_manager_hash_collision_handling() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let manager = CacheManager::new(&db_path).unwrap();

            let body = b"collision test";
            let initial = test_result("First.java", 3);
            let updated = test_result("Second.java", 5);

            manager.insert(body, &initial).unwrap();
            manager.insert(body, &updated).unwrap();

            let final_result = manager.query(body).unwrap().unwrap();
            assert_eq!(
                final_result.path, "Second.java",
                "later insert should overwrite earlier one"
            );
            assert_eq!(final_result.violation_count, 5);
        }

        #[test]
        fn test_manager_is_send_sync() {
            fn assert_send_sync<T: Send + Sync>() {}
            assert_send_sync::<CacheManager>();
        }

        #[test]
        fn test_manager_multiple_operations() {
            let tmp = tempfile::tempdir().unwrap();
            let db_path = tmp.path().join("test.db");
            let manager = CacheManager::new(&db_path).unwrap();

            let bodies = vec![
                b"public int method1() { return 0; }".to_vec(),
                b"public void method2() { }".to_vec(),
                b"public boolean method3() { return false; }".to_vec(),
            ];

            for (i, body) in bodies.iter().enumerate() {
                let result = test_result(&format!("File{}.java", i), i);
                manager.insert(body, &result).unwrap();
            }

            assert_eq!(
                manager.count().unwrap(),
                3,
                "should have cached 3 entries"
            );

            for body in bodies.iter() {
                assert!(manager.contains(body).unwrap());
            }
        }
    }
}

#[cfg(test)]
mod integration_tests {
    use super::manager::CacheManager;
    use super::store::CachedScanResult;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn test_result(path: &str, count: usize) -> CachedScanResult {
        let cached_at = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();
        CachedScanResult {
            path: path.to_string(),
            violation_count: count,
            cached_at,
        }
    }

    #[test]
    fn test_full_cache_lifecycle() {
        let tmp = tempfile::tempdir().unwrap();
        let db_path = tmp.path().join("lifecycle.db");
        let mgr = CacheManager::new(&db_path).unwrap();

        // Insert some methods
        let methods = vec![
            (b"public void alpha() { }".to_vec(), "Method1.java", 0),
            (
                b"public String beta() { return null; }".to_vec(),
                "Method2.java",
                1,
            ),
            (
                b"public int gamma() { // TODO\n return 0; }".to_vec(),
                "Method3.java",
                2,
            ),
        ];

        // Store all methods
        for (body, path, violations) in &methods {
            let result = test_result(path, *violations);
            mgr.insert(body, &result).unwrap();
        }

        // Verify all are cached
        for (body, path, _) in &methods {
            let cached = mgr.query(body).unwrap();
            assert!(cached.is_some());
            assert_eq!(cached.unwrap().path, *path);
        }

        // Verify count
        assert_eq!(mgr.count().unwrap(), 3);
    }

    #[test]
    fn test_persistence_across_instances() {
        let tmp = tempfile::tempdir().unwrap();
        let db_path = tmp.path().join("persistent.db");

        let body = b"public void persist() { }";
        let result = test_result("Persistent.java", 0);

        // First instance: write
        {
            let mgr1 = CacheManager::new(&db_path).unwrap();
            mgr1.insert(body, &result).unwrap();
            assert_eq!(mgr1.count().unwrap(), 1);
        }

        // Second instance: read
        {
            let mgr2 = CacheManager::new(&db_path).unwrap();
            assert_eq!(mgr2.count().unwrap(), 1);
            let cached = mgr2.query(body).unwrap();
            assert!(cached.is_some());
            assert_eq!(cached.unwrap().path, "Persistent.java");
        }
    }
}
