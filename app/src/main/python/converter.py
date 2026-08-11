from markitdown import MarkItDown


def convert(path: str) -> str:
    """Convert a local PDF file to Markdown text using markitdown."""
    md = MarkItDown(enable_plugins=False)
    result = md.convert_local(path)
    return result.text_content
