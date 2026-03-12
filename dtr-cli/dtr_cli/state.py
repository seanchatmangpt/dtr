"""Global CLI runtime state — single source of truth."""
from dataclasses import dataclass, field
from rich.console import Console


@dataclass
class CLIState:
    json_mode: bool = False
    quiet: bool = False
    verbose: bool = False
    console: Console = field(default_factory=lambda: Console(stderr=True))
    out: Console = field(default_factory=lambda: Console())


# Module-level singleton
_state = CLIState()


def get_state() -> CLIState:
    return _state


def configure(
    json_mode: bool = False,
    quiet: bool = False,
    verbose: bool = False,
    no_color: bool = False,
) -> None:
    _state.json_mode = json_mode
    _state.quiet = quiet
    _state.verbose = verbose
    _state.console = Console(stderr=True, no_color=no_color or json_mode, quiet=quiet)
    _state.out = Console(no_color=no_color or json_mode)
