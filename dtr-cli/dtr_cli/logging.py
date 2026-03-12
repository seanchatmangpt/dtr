"""
Structured logging for DTR CLI.

In human mode: Rich-formatted output to stderr.
In JSON mode: JSON Lines to stderr (one JSON object per log record).

Usage:
    from dtr_cli.logging import configure_logging, get_logger

    # Call once at CLI startup
    configure_logging(json_mode=False, verbose=False)

    # Use in any module
    logger = get_logger(__name__)
    logger.info("Build started")
    logger.error("Maven failed", extra={"returncode": 1})
"""

import json
import logging
import sys
from typing import Any


class JsonLinesHandler(logging.Handler):
    """Emit log records as JSON Lines to stderr.

    One JSON object per line — suitable for log aggregation pipelines
    (Datadog, Splunk, CloudWatch Logs Insights, etc.).
    """

    def emit(self, record: logging.LogRecord) -> None:
        entry: dict[str, Any] = {
            "level": record.levelname,
            "logger": record.name,
            "message": self.format(record),
            "time": record.created,
        }
        if record.exc_info:
            entry["exception"] = logging.Formatter().formatException(record.exc_info)
        # Flush immediately so lines appear in order even when piped.
        print(json.dumps(entry), file=sys.stderr, flush=True)


class RichHandler(logging.Handler):
    """Emit log records using Rich markup to stderr.

    Falls back gracefully when Rich is not available (outputs plain text).
    """

    def __init__(self, console: Any = None) -> None:
        super().__init__()
        self._console = console

    def emit(self, record: logging.LogRecord) -> None:
        try:
            from rich.console import Console

            console = self._console or Console(stderr=True)
            level_styles: dict[str, str] = {
                "DEBUG": "[dim]",
                "INFO": "[blue]",
                "WARNING": "[yellow]",
                "ERROR": "[red]",
                "CRITICAL": "[bold red]",
            }
            style = level_styles.get(record.levelname, "")
            msg = self.format(record)
            if style:
                console.print(
                    f"{style}{record.levelname}[/]: {msg}",
                    markup=True,
                    highlight=False,
                )
            else:
                console.print(f"{record.levelname}: {msg}", markup=False)
        except Exception:
            # Never let logging machinery crash the CLI.
            self.handleError(record)


def get_logger(name: str) -> logging.Logger:
    """Return a configured logger for the given module name.

    All DTR loggers live under the ``dtr.*`` namespace so they inherit
    the handler configured by :func:`configure_logging`.

    Args:
        name: Short module name (e.g. ``"build"``, ``"publish"``).
              The returned logger will be named ``dtr.<name>``.

    Returns:
        A :class:`logging.Logger` instance.
    """
    return logging.getLogger(f"dtr.{name}")


def configure_logging(
    json_mode: bool = False,
    verbose: bool = False,
    console: Any = None,
) -> None:
    """Configure the root DTR logger.

    Call once at CLI startup (typically in ``main()`` before any command
    runs). Subsequent calls to :func:`get_logger` automatically inherit
    this configuration.

    Args:
        json_mode: When ``True`` emit JSON Lines to stderr for machine
                   consumption.  When ``False`` use Rich-formatted output.
        verbose:   When ``True`` set level to DEBUG; otherwise INFO.
        console:   Optional :class:`rich.console.Console` instance to use
                   in human mode (useful for testing).
    """
    root = logging.getLogger("dtr")
    # Remove any previously registered handlers so reconfiguration is safe.
    root.handlers.clear()
    root.setLevel(logging.DEBUG if verbose else logging.INFO)

    if json_mode:
        handler: logging.Handler = JsonLinesHandler()
    else:
        handler = RichHandler(console=console)

    handler.setFormatter(logging.Formatter("%(message)s"))
    root.addHandler(handler)
    # Prevent records from leaking to the root Python logger.
    root.propagate = False
