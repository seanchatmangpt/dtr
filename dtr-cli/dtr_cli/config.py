"""DTR CLI Configuration Manager.

Searches for .dtr.yml starting from the current directory, then walks up
parent directories until a pom.xml is found (project root boundary).

Config sections:
  build:   modules, skip_tests, verbose
  export:  output_dir, formats
  publish: platform, target
  report:  type, output_dir
"""

from __future__ import annotations

import copy
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Any, Optional

import yaml


# ---------------------------------------------------------------------------
# Typed config dataclasses
# ---------------------------------------------------------------------------


@dataclass
class BuildConfig:
    modules: list[str] = field(default_factory=list)
    skip_tests: bool = False
    verbose: bool = False


@dataclass
class ExportConfig:
    output_dir: str = "target/docs/test-results"
    formats: list[str] = field(default_factory=lambda: ["markdown", "html"])


@dataclass
class PublishConfig:
    platform: str = "local"
    target: str = "."


@dataclass
class ReportConfig:
    type: str = "summary"
    output_dir: str = "target/docs/reports"


@dataclass
class DtrConfig:
    build: BuildConfig = field(default_factory=BuildConfig)
    export: ExportConfig = field(default_factory=ExportConfig)
    publish: PublishConfig = field(default_factory=PublishConfig)
    report: ReportConfig = field(default_factory=ReportConfig)

    # Track where this config was loaded from (None = defaults only)
    _source: Optional[Path] = field(default=None, repr=False, compare=False)

    @classmethod
    def defaults(cls) -> "DtrConfig":
        """Return a config populated entirely with default values."""
        return cls()

    def to_dict(self) -> dict[str, Any]:
        """Serialize to a plain dict suitable for YAML output."""
        raw = asdict(self)
        raw.pop("_source", None)
        return raw

    def source(self) -> Optional[Path]:
        """Return the path of the .dtr.yml that was loaded, or None."""
        return self._source


# ---------------------------------------------------------------------------
# Config manager
# ---------------------------------------------------------------------------

CONFIG_FILENAME = ".dtr.yml"


def _find_project_root(start: Path) -> Optional[Path]:
    """Walk up from *start* until pom.xml is found.  Return that directory."""
    current = start.resolve()
    for _ in range(20):
        if (current / "pom.xml").exists():
            return current
        parent = current.parent
        if parent == current:
            break
        current = parent
    return None


def _find_config_file(start: Path) -> Optional[Path]:
    """Search for .dtr.yml from *start* up to the project root (pom.xml dir).

    Returns the first .dtr.yml found, or None if none exists.
    """
    current = start.resolve()
    project_root = _find_project_root(current)

    for _ in range(20):
        candidate = current / CONFIG_FILENAME
        if candidate.exists():
            return candidate
        if project_root is not None and current == project_root:
            break
        parent = current.parent
        if parent == current:
            break
        current = parent
    return None


def _section_from_dict(cls: type, data: dict[str, Any]) -> Any:
    """Construct a dataclass from a dict, ignoring unknown keys."""
    known = {f.name for f in cls.__dataclass_fields__.values()}  # type: ignore[attr-defined]
    filtered = {k: v for k, v in data.items() if k in known}
    return cls(**filtered)


def load_config(start: Optional[Path] = None) -> DtrConfig:
    """Load and return the resolved DtrConfig.

    1. Start with defaults.
    2. Find .dtr.yml (current dir → project root).
    3. Merge file values over defaults.

    Args:
        start: Directory to start searching from.  Defaults to cwd.

    Returns:
        Fully resolved DtrConfig instance.
    """
    search_dir = start if start is not None else Path.cwd()
    config_path = _find_config_file(search_dir)

    cfg = DtrConfig.defaults()

    if config_path is None:
        return cfg

    try:
        raw = yaml.safe_load(config_path.read_text()) or {}
    except yaml.YAMLError as exc:
        raise ValueError(f"Invalid YAML in {config_path}: {exc}") from exc

    if not isinstance(raw, dict):
        raise ValueError(f"Expected a YAML mapping in {config_path}, got {type(raw).__name__}")

    if "build" in raw and isinstance(raw["build"], dict):
        cfg.build = _section_from_dict(BuildConfig, raw["build"])
    if "export" in raw and isinstance(raw["export"], dict):
        cfg.export = _section_from_dict(ExportConfig, raw["export"])
    if "publish" in raw and isinstance(raw["publish"], dict):
        cfg.publish = _section_from_dict(PublishConfig, raw["publish"])
    if "report" in raw and isinstance(raw["report"], dict):
        cfg.report = _section_from_dict(ReportConfig, raw["report"])

    cfg._source = config_path
    return cfg


def save_config(cfg: DtrConfig, path: Path) -> None:
    """Write *cfg* to *path* as YAML.

    Args:
        cfg:  Config to serialize.
        path: Destination file (will be created or overwritten).
    """
    path.write_text(yaml.dump(cfg.to_dict(), default_flow_style=False, sort_keys=True))


def init_config(directory: Optional[Path] = None) -> Path:
    """Create a default .dtr.yml in *directory* (default: cwd).

    Raises:
        FileExistsError: If .dtr.yml already exists.

    Returns:
        Path to the created file.
    """
    target_dir = directory if directory is not None else Path.cwd()
    target = target_dir / CONFIG_FILENAME
    if target.exists():
        raise FileExistsError(f"{target} already exists")
    save_config(DtrConfig.defaults(), target)
    return target


# ---------------------------------------------------------------------------
# Dot-notation get / set helpers
# ---------------------------------------------------------------------------

_SECTION_MAP = {
    "build": "build",
    "export": "export",
    "publish": "publish",
    "report": "report",
}


def get_value(cfg: DtrConfig, key: str) -> Any:
    """Return the value at *key* using dot notation (e.g. ``build.verbose``).

    Raises:
        KeyError: If the key or section does not exist.
    """
    parts = key.split(".", 1)
    if len(parts) == 1:
        # Top-level section requested — return its dict
        section_name = parts[0]
        if section_name not in _SECTION_MAP:
            raise KeyError(f"Unknown config section: '{section_name}'")
        return asdict(getattr(cfg, section_name))

    section_name, attr = parts
    if section_name not in _SECTION_MAP:
        raise KeyError(f"Unknown config section: '{section_name}'")
    section = getattr(cfg, section_name)
    if not hasattr(section, attr):
        raise KeyError(f"Unknown config key: '{key}'")
    return getattr(section, attr)


def set_value(cfg: DtrConfig, key: str, value: str) -> None:
    """Set the value at *key* (dot notation) by coercing *value* to the right type.

    Modifies *cfg* in-place.

    Raises:
        KeyError:   If the section or attribute does not exist.
        ValueError: If the value cannot be coerced.
    """
    parts = key.split(".", 1)
    if len(parts) != 2:
        raise KeyError(f"Key must be 'section.attribute', got: '{key}'")

    section_name, attr = parts
    if section_name not in _SECTION_MAP:
        raise KeyError(f"Unknown config section: '{section_name}'")
    section = getattr(cfg, section_name)
    if not hasattr(section, attr):
        raise KeyError(f"Unknown config key: '{key}'")

    current = getattr(section, attr)
    coerced = _coerce(value, current)
    setattr(section, attr, coerced)


def _coerce(raw: str, reference: Any) -> Any:
    """Coerce *raw* string to the same type as *reference*."""
    if isinstance(reference, bool):
        if raw.lower() in ("true", "1", "yes"):
            return True
        if raw.lower() in ("false", "0", "no"):
            return False
        raise ValueError(f"Cannot interpret '{raw}' as bool")
    if isinstance(reference, int):
        return int(raw)
    if isinstance(reference, list):
        # Accept comma-separated values
        return [item.strip() for item in raw.split(",") if item.strip()]
    return raw  # str
