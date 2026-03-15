/// Data models for Javadoc extraction.
///
/// This module defines the core data structures for representing extracted
/// documentation at the method and module levels, plus per-file results.

use serde::{Deserialize, Serialize};
use crate::error::DocViolation;

/// Method-level Javadoc entry with description, parameters, returns, throws, and tags.
#[derive(Serialize, Deserialize, Debug, Default, PartialEq)]
pub struct JavadocEntry {
    pub description: String,
    pub params: Vec<ParamDoc>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub returns: Option<String>,
    pub throws: Vec<ThrowsDoc>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub since: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecated: Option<String>,
    pub see: Vec<String>,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
pub struct ParamDoc {
    pub name: String,
    pub description: String,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
pub struct ThrowsDoc {
    pub exception: String,
    pub description: String,
}

/// Documentation for a top-level Java type (class, interface, record, enum).
#[derive(Debug, Default)]
pub struct ModuleDoc {
    /// Fully qualified class name, e.g. `io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands`
    pub fqcn: String,
    /// Simple class name
    pub simple_name: String,
    /// Package
    pub package: String,
    /// The cleaned Javadoc description (license headers stripped)
    pub description: String,
    /// `@since` value, if present
    pub since: Option<String>,
    /// `@deprecated` text, if present
    pub deprecated: Option<String>,
    /// `@see` references
    pub see: Vec<String>,
    /// Java signature of the type declaration (everything before the opening `{`)
    pub signature: String,
    /// Kind: "class", "interface", "record", "enum"
    pub kind: String,
    /// Method count (for the summary comment in the code block)
    pub method_count: usize,
    /// Method names (first 10, for the summary)
    pub method_names: Vec<String>,
}

/// Per-file extraction result: module doc, method docs, and violations.
pub struct FileDocResult {
    pub module_doc: Option<ModuleDoc>,
    pub method_docs: Vec<(String, JavadocEntry)>,
    pub violations: Vec<DocViolation>,
}
