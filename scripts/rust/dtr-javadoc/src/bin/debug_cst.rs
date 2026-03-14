fn main() {
    let comment = r#"/**
     * Risky operation.
     *
     * @throws IllegalStateException if not ready
     * @param input the input
     * @return result
     */"#;

    let mut parser = tree_sitter::Parser::new();
    parser.set_language(&tree_sitter_javadoc::LANGUAGE.into()).unwrap();
    let tree = parser.parse(comment.as_bytes(), None).unwrap();
    let root = tree.root_node();
    print_node(&root, comment.as_bytes(), 0);
}

fn print_node(node: &tree_sitter::Node, source: &[u8], depth: usize) {
    let indent = "  ".repeat(depth);
    let text = node.utf8_text(source).unwrap_or("?");
    let preview: String = text.chars().take(60).map(|c| if c == '\n' { '|' } else { c }).collect();
    println!("{}[{}] kind={:?}", indent, depth, node.kind());
    println!("{}  {:?}", indent, preview);
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        print_node(&child, source, depth + 1);
    }
}
