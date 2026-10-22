Rogler Core v0.7.3 – server templates for Xaero cave mode and radar privacy

Copy minimap/server_profiles/default.cfg into:
  <server>/config/xaero/minimap/server_profiles/default.cfg
Copy world-map/server_profiles/default.cfg into:
  <server>/config/xaero/world-map/server_profiles/default.cfg

Cave Mode is allowed, player/entity radar off. The old fair-play chat marker is no longer sent.
Ensure Xaero Minimap and World Map are installed SERVER-SIDE and enforce the default
server profile. Reconnect clients / restart after changing. Do not bypass server
profile enforcement permission unless you intend admins to override radar.

Separately edit <world>/serverconfig/ftbchunks-world.snbt (see docs/archive/readmes/README_Rogler_Core_v0.7.3.md).
