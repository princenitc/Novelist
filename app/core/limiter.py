"""Central slowapi Limiter instance shared across the application."""
from slowapi import Limiter
from slowapi.util import get_remote_address

# Key function: identify callers by their IP address.
# In production behind a proxy, replace with a function that reads X-Forwarded-For.
limiter = Limiter(key_func=get_remote_address, default_limits=["200/minute"])
