import traceback


def convert(path: str) -> str:
    try:
        from markitdown import MarkItDown
    except Exception:
        return "IMPORT ERROR:\n" + traceback.format_exc()

    try:
        md = MarkItDown(enable_plugins=False)
        result = md.convert_local(path)
        return result.text_content
    except Exception:
        return "CONVERT ERROR:\n" + traceback.format_exc()
