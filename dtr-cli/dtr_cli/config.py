"""DTR CLI Configuration Manager.

Searches for .dtr.yml starting from the current directory, then walks up
parent directories until a pom.xml is found (project root boundary).

Config sections:
  build:   goals, profiles, skip_tests, verbose, timeout_seconds, modules
  export:  output_dir, formats
  publish: platform, target
  report:  type, output_dir
"""

from __future__ import annotations

from pathlib import Path
from typing import Any, Optional

import yaml
from pydantic import BaseModel, field_validator, model_validator

from dtr_cli.cli_errors import ConfigurationError


# ---------------------------------------------------------------------------
# Pydantic v2 config models
# ---------------------------------------------------------------------------


class BuildConfig(BaseModel):
    """Build section: controls Maven invocation."""

    goals: str = "clean verify"
    profiles: list[str] = []
    verbose: bool = False
    timeout_seconds: int = 600
    modules: list[str] = []
    skip_tests: bool = False

    @field_validator("timeout_seconds")
    @classmethod
    def timeout_must_be_positive(cls, v: int) -> int:
        if v <= 0:
            raise ValueError(f"timeout_seconds must be positive, got {v}")
        return v

    @field_validator("goals")
    @classmethod
    def goals_must_not_be_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("goals must not be empty")
        return v.strip()


class ExportConfig(BaseModel):
    """Export section: output location and enabled formats."""

    output_dir: str = "target/docs/test-results"
    formats: list[str] = ["markdown", "html"]

    @field_validator("output_dir")
    @classmethod
    def output_dir_must_not_be_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("output_dir must not be empty")
        return v.strip()

    @field_validator("formats")
    @classmethod
    def formats_must_not_be_empty(cls, v: list[str]) -> list[str]:
        if len(v) == 0:
            raise ValueError("formats must contain at least one format")
        return v


class PublishConfig(BaseModel):
    """Publish section: destination platform and target path."""

    platform: str = "local"
    target: str = "."

    VALID_PLATFORMS: list[str] = ["local", "github", "s3", "gcs", "nexus"]

    @field_validator("platform")
    @classmethod
    def platform_must_be_known(cls, v: str) -> str:
        valid = {"local", "github", "s3", "gcs", "nexus"}
        if v not in valid:
            raise ValueError(f"platform must be one of {sorted(valid)}, got '{v}'")
        return v

    model_config = {"arbitrary_types_allowed": True}


class ReportConfig(BaseModel):
    """Report section: report type and output location."""

    type: str = "summary"
    output_dir: str = "target/docs/reports"

    VALID_TYPES: list[str] = ["summary", "coverage", "log", "full"]

    @field_validator("type")
    @classmethod
    def type_must_be_known(cls, v: str) -> str:
        valid = {"summary", "coverage", "log", "full"}
        if v not in valid:
            raise ValueError(f"report type must be one of {sorted(valid)}, got '{v}'")
        return v

    @field_validator("output_dir")
    @classmethod
    def output_dir_must_not_be_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("output_dir must not be empty")
        return v.strip()

    model_config = {"arbitrary_types_allowed": True}


class DtrConfig(BaseModel):
    """Top-level DTR configuration document."""

    build: BuildConfig = BuildConfig()
    export: ExportConfig = ExportConfig()
    publish: PublishConfig = PublishConfig()
    report: ReportConfig = ReportConfig()

    # Non-persisted: tracks origin file path
    _source: Optional[Path] = None

    model_config = {"arbitrary_types_allowed": True}

    @classmethod
    def defaults(cls) -> "DtrConfig":
        """Return a config populated entirely with default values."""
        return cls()

    def to_dict(self) -> dict[str, Any]:
        """Serialize to a plain dict suitable for YAML output.

        Excludes the private _source tracking field.
        """
        return {
            "build": self.build.model_dump(exclude={"VALID_PLATFORMS"}),
            "export": self.export.model_dump(),
            "publish": self.publish.model_dump(exclude={"VALID_PLATFORMS"}),
            "report": self.report.model_dump(exclude={"VALID_TYPES"}),
        }

    def source(self) -> Optional[Path]:
        """Return the path of the .dtr.yml that was loaded, or None."""
        return self._source

    def _set_source(self, path: Path) -> None:
        """Internal: record the file this config was loaded from."""
        object.__setattr__(self, "_source", path)


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


def _build_config_from_dict(raw: dict[str, Any]) -> DtrConfig:
    """Construct a DtrConfig from a raw dict.

    Raises:
        ConfigurationError: If Pydantic validation fails (with field-level details).
    """
    import pydantic

    section_data: dict[str, Any] = {}

    for section in ("build", "export", "publish", "report"):
        if section in raw and isinstance(raw[section], dict):
            section_data[section] = raw[section]

    try:
        return DtrConfig(**section_data)
    except pydantic.ValidationError as exc:
        # Collect all field-level errors into an actionable message
        field_errors: list[str] = []
        for err in exc.errors():
            loc = ".".join(str(p) for p in err["loc"])
            msg = err["msg"]
            field_errors.append(f"  {loc}: {msg}")
        detail = "\n".join(field_errors)
        raise ConfigurationError(
            message=f"Configuration validation failed:\n{detail}",
            context={"field_errors": field_errors},
        ) from exc


def load_config(start: Optional[Path] = None) -> DtrConfig:
    """Load and return the resolved DtrConfig.

    1. Start with defaults.
    2. Find .dtr.yml (current dir → project root).
    3. Validate and merge file values over defaults.

    Args:
        start: Directory to start searching from.  Defaults to cwd.

    Returns:
        Fully resolved DtrConfig instance.

    Raises:
        ConfigurationError: If .dtr.yml contains invalid values.
        ValueError: If .dtr.yml is not valid YAML or not a mapping.
    """
    search_dir = start if start is not None else Path.cwd()
    config_path = _find_config_file(search_dir)

    if config_path is None:
        return DtrConfig.defaults()

    try:
        raw = yaml.safe_load(config_path.read_text()) or {}
    except yaml.YAMLError as exc:
        raise ValueError(f"Invalid YAML in {config_path}: {exc}") from exc

    if not isinstance(raw, dict):
        raise ValueError(
            f"Expected a YAML mapping in {config_path}, got {type(raw).__name__}"
        )

    # Strip unknown top-level keys so Pydantic doesn't see them
    known_sections = {"build", "export", "publish", "report"}
    filtered: dict[str, Any] = {k: v for k, v in raw.items() if k in known_sections}

    # For each section, strip unknown keys (preserve backward-compat)
    for section in list(filtered.keys()):
        if isinstance(filtered[section], dict):
            model_cls: type[BaseModel] | None = {
                "build": BuildConfig,
                "export": ExportConfig,
                "publish": PublishConfig,
                "report": ReportConfig,
            }.get(section)
            if model_cls is not None:
                known_fields = set(model_cls.model_fields.keys())
                filtered[section] = {
                    k: v
                    for k, v in filtered[section].items()
                    if k in known_fields
                }

    cfg = _build_config_from_dict(filtered)
    # Use private attribute bypass to avoid Pydantic treating _source as a field
    object.__setattr__(cfg, "_source", config_path)
    return cfg


def validate_config(config_path: Optional[Path] = None) -> list[str]:
    """Validate config and return list of error messages.

    An empty return list means the config is valid.

    Args:
        config_path: Explicit path to a .dtr.yml file to validate, or None
                     to search from the current working directory.

    Returns:
        List of human-readable error strings.  Empty = valid.
    """
    errors: list[str] = []
    try:
        if config_path is not None:
            # Load directly from the provided file path's parent directory
            load_config(config_path.parent)
        else:
            load_config(None)
    except ConfigurationError as e:
        errors.append(str(e))
    except ValueError as e:
        errors.append(str(e))
    return errors


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
        section = getattr(cfg, section_name)
        # Return public fields only
        return section.model_dump(
            exclude={"VALID_PLATFORMS", "VALID_TYPES"}
        )

    section_name, attr = parts
    if section_name not in _SECTION_MAP:
        raise KeyError(f"Unknown config section: '{section_name}'")
    section = getattr(cfg, section_name)
    if attr not in section.model_fields:
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
    if attr not in section.model_fields:
        raise KeyError(f"Unknown config key: '{key}'")

    current = getattr(section, attr)
    coerced = _coerce(value, current)
    # Pydantic models are immutable by default — use model_copy to get a new one
    # but since we need in-place mutation for the existing API, we bypass:
    object.__setattr__(section, attr, coerced)


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
