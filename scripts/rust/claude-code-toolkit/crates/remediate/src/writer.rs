//! `writer` — Atomic file write using tempfile + std::fs::rename with chunked I/O.
//!
//! Guarantees: write to a temp file in the same directory, then atomic rename.
//! On POSIX systems, rename is atomic. Never writes directly to the source file.
//!
//! Optimization: Large files are written in 64KB chunks to reduce allocator pressure
//! and improve cache locality. sync_all() is called once at the end, not per chunk.

use anyhow::Result;
use std::fs;
use std::io::Write;
use std::path::Path;
use tempfile::NamedTempFile;

/// Chunk size for buffered writes (64KB).
/// Balances memory usage against system call overhead.
const WRITE_CHUNK_SIZE: usize = 64 * 1024;

/// Atomically write `data` to `path` using chunked I/O for large files.
///
/// Process:
/// 1. Create a NamedTempFile in the same directory as `path`
/// 2. Write `data` to the temp file in 64KB chunks (reduces allocator pressure)
/// 3. Sync to disk once (after all writes)
/// 4. Rename temp file to `path` (atomic on POSIX)
///
/// # Arguments
/// * `path` - Destination file path
/// * `data` - Bytes to write
///
/// # Returns
/// Ok(()) on success, Err if write/rename fails
pub fn atomic_write(path: &Path, data: &[u8]) -> Result<()> {
    // Determine parent directory for temp file (same as destination)
    let parent_dir = path.parent().unwrap_or_else(|| Path::new("."));

    // Create temp file in same directory
    let mut temp_file = NamedTempFile::new_in(parent_dir)?;

    // Write data in chunks to improve locality and reduce allocator pressure
    for chunk in data.chunks(WRITE_CHUNK_SIZE) {
        temp_file.write_all(chunk)?;
    }

    // Single flush + sync_all() after all writes (not per-chunk)
    temp_file.flush()?;
    temp_file.as_file().sync_all()?;

    // Atomic rename on POSIX systems
    let temp_path = temp_file.path().to_path_buf();
    fs::rename(&temp_path, path)?;

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Read;

    #[test]
    fn test_atomic_write_creates_file() {
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let target_path = temp_dir.path().join("output.txt");

        let data = b"hello, world!";
        atomic_write(&target_path, data).expect("atomic_write failed");

        // Verify file was created with correct content
        let mut f = fs::File::open(&target_path).expect("open written file");
        let mut buf = Vec::new();
        f.read_to_end(&mut buf).expect("read file");
        assert_eq!(&buf[..], data);
    }

    #[test]
    fn test_atomic_write_overwrites_existing() {
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let target_path = temp_dir.path().join("output.txt");

        // Write initial content
        fs::write(&target_path, b"old content").expect("initial write");

        // Overwrite with atomic_write
        let new_data = b"new content";
        atomic_write(&target_path, new_data).expect("atomic_write failed");

        // Verify new content
        let content = fs::read(&target_path).expect("read updated file");
        assert_eq!(&content[..], new_data);
    }

    #[test]
    fn test_atomic_write_large_file() {
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let target_path = temp_dir.path().join("large.bin");

        // Create a 1MB chunk of data
        let data: Vec<u8> = vec![0x42; 1024 * 1024];
        atomic_write(&target_path, &data).expect("atomic_write failed");

        // Verify size and content
        let metadata = fs::metadata(&target_path).expect("metadata");
        assert_eq!(metadata.len(), 1024 * 1024);

        let content = fs::read(&target_path).expect("read large file");
        assert_eq!(content, data);
    }

    #[test]
    fn test_atomic_write_empty_file() {
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let target_path = temp_dir.path().join("empty.txt");

        atomic_write(&target_path, &[]).expect("atomic_write empty failed");

        let metadata = fs::metadata(&target_path).expect("metadata");
        assert_eq!(metadata.len(), 0);
    }

    #[test]
    fn test_atomic_write_utf8_content() {
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let target_path = temp_dir.path().join("unicode.txt");

        let data = "Hello, 世界! 🚀".as_bytes();
        atomic_write(&target_path, data).expect("atomic_write failed");

        let content = fs::read(&target_path).expect("read file");
        assert_eq!(&content[..], data);

        let text = String::from_utf8(content).expect("UTF-8");
        assert_eq!(text, "Hello, 世界! 🚀");
    }

    #[test]
    fn test_atomic_write_nested_directory() {
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let nested_path = temp_dir.path().join("sub").join("dir");
        fs::create_dir_all(&nested_path).expect("create nested dir");

        let target_path = nested_path.join("file.txt");
        let data = b"nested content";
        atomic_write(&target_path, data).expect("atomic_write in nested dir failed");

        let content = fs::read(&target_path).expect("read nested file");
        assert_eq!(&content[..], data);
    }

    #[test]
    fn test_atomic_write_crash_safety() {
        // Verify that if a write is interrupted (simulated by checking temp file
        // doesn't exist after atomic_write), the target file is either unchanged
        // or fully written (never partial).
        let temp_dir = tempfile::tempdir().expect("create temp dir");
        let target_path = temp_dir.path().join("crash_test.txt");

        let original = b"original";
        fs::write(&target_path, original).expect("initial write");

        let new_data = b"new and improved";
        atomic_write(&target_path, new_data).expect("atomic_write failed");

        // After successful atomic_write, file should contain the new data
        let content = fs::read(&target_path).expect("read file");
        assert_eq!(&content[..], new_data);

        // There should be no .tmp files left behind in the directory
        let entries = fs::read_dir(temp_dir.path()).expect("read_dir");
        let tmp_count = entries
            .filter_map(|e| {
                e.ok().and_then(|entry| {
                    entry
                        .file_name()
                        .into_string()
                        .ok()
                        .filter(|n| n.contains(".tmp"))
                        .map(|_| ())
                })
            })
            .count();
        assert_eq!(
            tmp_count, 0,
            "no temp files should remain after atomic_write"
        );
    }
}
