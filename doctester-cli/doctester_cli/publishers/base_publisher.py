"""Base publisher class."""

from abc import ABC, abstractmethod
from doctester_cli.model import PublishConfig, PublishResult


class BasePublisher(ABC):
    """Abstract base class for publishers."""

    @abstractmethod
    def publish(self, config: PublishConfig) -> PublishResult:
        """Publish exports according to configuration."""
        pass
