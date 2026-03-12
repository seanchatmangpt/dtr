"""
Chicago TDD: verify the error code contract.

Every CLIError subclass must have a unique DTR-NNN error code.
This is a contract test — it enforces our error taxonomy.
"""
import inspect
import json
import pytest
from dtr_cli.cli_errors import CLIError


def _get_all_error_classes():
    """Collect all CLIError subclasses from the module."""
    from dtr_cli import cli_errors
    classes = []
    for name, obj in inspect.getmembers(cli_errors, inspect.isclass):
        if issubclass(obj, CLIError) and obj is not CLIError:
            classes.append((name, obj))
    return classes


@pytest.mark.parametrize("name,cls", _get_all_error_classes())
def test_error_class_has_error_code(name, cls):
    """Every error class must have a non-default error code."""
    # Try to get error_code from class or instance
    code = getattr(cls, 'error_code', None)
    if code == "DTR-000" or code is None:
        # Try instantiating
        try:
            instance = cls(message="test") if 'message' in inspect.signature(cls.__init__).parameters else cls()
            code = instance.error_code
        except Exception:
            pass
    assert code is not None, f"{name} must have error_code attribute"
    assert code != "DTR-000", f"{name} must have unique error_code, not default DTR-000"
    assert code.startswith("DTR-"), f"{name}.error_code must follow DTR-NNN format, got: {code}"


def test_all_error_codes_are_unique():
    """No two error classes may share the same error code."""
    codes = {}
    for name, cls in _get_all_error_classes():
        code = getattr(cls, 'error_code', None)
        if code and code != "DTR-000":
            assert code not in codes, (
                f"Duplicate error_code {code}: {name} and {codes[code]}"
            )
            codes[code] = name


def test_format_json_produces_valid_json():
    """format_json() must always produce valid JSON."""
    err = CLIError(message="test error", hint="do this", error_code="DTR-999")
    output = err.format_json()
    parsed = json.loads(output)
    assert parsed["error"] == "DTR-999"
    assert parsed["message"] == "test error"
    assert parsed["hint"] == "do this"
    assert "context" in parsed


def test_format_message_contains_emoji_and_code():
    """format_message() must contain error indicator and code."""
    err = CLIError(message="something broke", error_code="DTR-888")
    msg = err.format_message()
    assert "DTR-888" in msg
    assert "something broke" in msg


def test_error_with_context_preserves_context_in_json():
    """Context data must survive serialization."""
    err = CLIError(
        message="build failed",
        error_code="DTR-203",
        context={"returncode": 1, "goals": "clean verify", "module": "dtr-core"}
    )
    parsed = json.loads(err.format_json())
    assert parsed["context"]["returncode"] == 1
    assert parsed["context"]["goals"] == "clean verify"
    assert parsed["context"]["module"] == "dtr-core"
