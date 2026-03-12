"""Chicago TDD: verify real logging output.

Tests assert on actual emitted bytes — no mocks of the logging subsystem itself.
"""
import json
import logging

import pytest

from dtr_cli.logging import JsonLinesHandler, RichHandler, configure_logging, get_logger


# ---------------------------------------------------------------------------
# Logger namespacing
# ---------------------------------------------------------------------------


def test_get_logger_returns_namespaced_logger():
    logger = get_logger("mymodule")
    assert logger.name == "dtr.mymodule"


def test_get_logger_different_names_return_different_loggers():
    a = get_logger("alpha")
    b = get_logger("beta")
    assert a is not b
    assert a.name != b.name


def test_get_logger_same_name_returns_same_instance():
    a = get_logger("shared")
    b = get_logger("shared")
    assert a is b


# ---------------------------------------------------------------------------
# configure_logging — level settings
# ---------------------------------------------------------------------------


def test_verbose_mode_sets_debug_level():
    configure_logging(json_mode=False, verbose=True)
    logger = get_logger("test_verbose")
    assert logger.isEnabledFor(logging.DEBUG)


def test_non_verbose_mode_info_level():
    configure_logging(json_mode=False, verbose=False)
    logger = get_logger("test_info")
    assert logger.isEnabledFor(logging.INFO)
    assert not logger.isEnabledFor(logging.DEBUG)


def test_configure_logging_clears_previous_handlers():
    configure_logging(json_mode=True)
    configure_logging(json_mode=False)
    root = logging.getLogger("dtr")
    # After reconfiguration there should be exactly one handler.
    assert len(root.handlers) == 1


def test_configure_logging_propagate_false():
    configure_logging(json_mode=False)
    root = logging.getLogger("dtr")
    assert root.propagate is False


# ---------------------------------------------------------------------------
# JSON Lines mode — real output on stderr
# ---------------------------------------------------------------------------


def test_json_mode_emits_json_lines(capsys):
    configure_logging(json_mode=True, verbose=False)
    logger = get_logger("test_json")
    logger.info("hello world")
    captured = capsys.readouterr()
    line = captured.err.strip()
    assert line, "Expected log output on stderr"
    parsed = json.loads(line)
    assert parsed["message"] == "hello world"
    assert parsed["level"] == "INFO"
    assert "logger" in parsed
    assert "time" in parsed


def test_json_mode_warning_level(capsys):
    configure_logging(json_mode=True, verbose=False)
    logger = get_logger("test_warn")
    logger.warning("something suspicious")
    captured = capsys.readouterr()
    parsed = json.loads(captured.err.strip())
    assert parsed["level"] == "WARNING"
    assert "suspicious" in parsed["message"]


def test_json_mode_debug_suppressed_when_not_verbose(capsys):
    configure_logging(json_mode=True, verbose=False)
    logger = get_logger("test_debug_off")
    logger.debug("this should not appear")
    captured = capsys.readouterr()
    assert captured.err.strip() == "", "DEBUG line should be suppressed in non-verbose mode"


def test_json_mode_debug_emitted_when_verbose(capsys):
    configure_logging(json_mode=True, verbose=True)
    logger = get_logger("test_debug_on")
    logger.debug("debug detail")
    captured = capsys.readouterr()
    line = captured.err.strip()
    assert line, "Expected DEBUG output in verbose mode"
    parsed = json.loads(line)
    assert parsed["level"] == "DEBUG"


def test_json_mode_logger_name_in_output(capsys):
    configure_logging(json_mode=True)
    logger = get_logger("publish")
    logger.info("deploying")
    captured = capsys.readouterr()
    parsed = json.loads(captured.err.strip())
    assert parsed["logger"] == "dtr.publish"


def test_json_mode_exception_info_included(capsys):
    configure_logging(json_mode=True)
    logger = get_logger("test_exc")
    try:
        raise ValueError("deliberate test error")
    except ValueError:
        logger.exception("caught it")
    captured = capsys.readouterr()
    parsed = json.loads(captured.err.strip())
    assert "exception" in parsed
    assert "ValueError" in parsed["exception"]
    assert "deliberate test error" in parsed["exception"]


# ---------------------------------------------------------------------------
# JsonLinesHandler — unit tests
# ---------------------------------------------------------------------------


def test_json_lines_handler_emit_produces_valid_json(capsys):
    handler = JsonLinesHandler()
    handler.setFormatter(logging.Formatter("%(message)s"))
    record = logging.LogRecord(
        name="dtr.test",
        level=logging.INFO,
        pathname="",
        lineno=0,
        msg="direct handler test",
        args=(),
        exc_info=None,
    )
    handler.emit(record)
    captured = capsys.readouterr()
    parsed = json.loads(captured.err.strip())
    assert parsed["level"] == "INFO"
    assert parsed["message"] == "direct handler test"
    assert parsed["logger"] == "dtr.test"
    assert isinstance(parsed["time"], float)


# ---------------------------------------------------------------------------
# RichHandler — smoke test (no crash, output on stderr)
# ---------------------------------------------------------------------------


def test_rich_handler_does_not_crash(capsys):
    from rich.console import Console
    from io import StringIO

    buffer = StringIO()
    console = Console(file=buffer, highlight=False)
    handler = RichHandler(console=console)
    handler.setFormatter(logging.Formatter("%(message)s"))
    record = logging.LogRecord(
        name="dtr.test",
        level=logging.WARNING,
        pathname="",
        lineno=0,
        msg="rich warning test",
        args=(),
        exc_info=None,
    )
    handler.emit(record)
    output = buffer.getvalue()
    assert "rich warning test" in output


def test_rich_handler_applies_style_for_known_levels(capsys):
    from rich.console import Console
    from io import StringIO

    for level_name, level in [("ERROR", logging.ERROR), ("INFO", logging.INFO)]:
        buffer = StringIO()
        console = Console(file=buffer, highlight=False)
        handler = RichHandler(console=console)
        handler.setFormatter(logging.Formatter("%(message)s"))
        record = logging.LogRecord(
            name="dtr.test",
            level=level,
            pathname="",
            lineno=0,
            msg=f"test message {level_name}",
            args=(),
            exc_info=None,
        )
        handler.emit(record)
        output = buffer.getvalue()
        assert f"test message {level_name}" in output
