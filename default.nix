{ config, options, lib, pkgs, ... }:

with lib;
let cfg = options.modules.isntweb-home;
    user = "isntweb-home";
    group = "isntweb-home";

in {
  options.modules.home = with lib; {
    enable = mkEnableOption false;
  };

  config = mkIf cfg.enable {
    # create a user in which to run the web app
    users.users.isntweb-home = {
      inherit group;
      isSystemUser = true;
    };

    users.groups.vaultwarden = {};

    # configure a systemd service to launh it
    systemd.services.isntweb-home = {
      aliases = [ "isntweb-home" ];
      after = [ "network.target" ];
      path = with pkgs; [ openssl ];
      serviceConfig = {
        User = user;
        Group = group;
        ExecStart = "${isntweb-bundle}/bin/isntweb-serve";
        PrivateTmp = "true";
        PrivateDevices = "true";
        ProtectHome = "true";
        ProtectSystem = "strict";
        StateDirectory = "isntweb-home";
      };
      wantedBy = [ "multi-user.target" ];
    };
  };
}
