from abc import ABC, abstractmethod

class BaseRL(ABC):
    @abstractmethod
    def select_action(self):
        raise NotImplementedError
    
    @abstractmethod
    def update(self):
        raise NotImplementedError
    
    @abstractmethod
    def save(self):
        raise NotImplementedError
    
    @abstractmethod
    def load(self):
        raise NotImplementedError