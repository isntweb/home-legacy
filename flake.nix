{
  description = "isntweb super mega website";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
    utils.url = "github:numtide/flake-utils";
    rust-overlay.url = "github:oxalica/rust-overlay";
    naersk.url = "github:nix-community/naersk";
  };

  outputs = { self, nixpkgs, rust-overlay, utils, naersk, ... }:
    # utils.lib.eachDefaultSystem (system:
    let
      system = "x86_64-linux";

      inherit (lib) attrValues;
      pkgs = import nixpkgs {
        inherit system;
        overlays = [ rust-overlay.overlay ];
      };
      lib = pkgs.lib;

      rust_channel = pkgs.rust-bin.fromRustupToolchainFile ./rust-toolchain.toml;

      naersk-lib = naersk.lib."${system}".override {
        cargo = rust_channel;
        rustc = rust_channel;
      };

      # The rust server package
      isntweb-home-server = naersk-lib.buildPackage {
        pname = "isntweb-home-server";
        root = ./.;
      };

      # The static front-end as a derivation
      static-sources = pkgs.stdenv.mkDerivation {
        name = "isntweb-sources";
        src = ./.;

        phases = "installPhase";

        installPhase = ''
            mkdir -p $out/static
            cp -r ${./static}/* $out/static
          '';
      };

      # a script executable passing the root dir to the package
      isntweb-bundle = pkgs.writeShellScriptBin
        # port is the second arg
        "isntweb-serve" "${isntweb-home-server}/bin/isntweb-home-server ${static-sources}/static 6200";

      # the nixos module taht runs the whole deal!
      isntwebHomeModule = { config, options, lib, pkgs, ... }:
        with lib;
        let cfg = config.isntweb-home;
            user = "isntweb-home";
            group = "isntweb-home";
        in {
          options.isntweb-home = with lib; {
            enable = mkEnableOption false;
          };

          config = mkIf cfg.enable {
            # create a user in which to run the web app
            users.users.isntweb-home = {
              inherit group;
              isSystemUser = true;
            };

            users.groups.isntweb-home = {};

            # configure a systemd service to launh it
            systemd.services.isntweb-home = {
              enable = true;
              aliases = [ "isntweb-home" ];
              after = [ "network.target" ];
              path = with pkgs; [ openssl ];
              serviceConfig = {
                User = user;
                Group = group;
                ExecStart = "${pkgs.sh} -c '${isntweb-bundle}/bin/isntweb-serve'";
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
      ;

    in rec {
      # packages.isntweb-home = isntweb-bundle;
      # apps.isntweb-home = packages.isntweb-home;
      defaultPackage.${system} = isntweb-bundle;
      nixosModules.isntweb-home = isntwebHomeModule;
      nixosModule = nixosModules.isntweb-home;

      devShell.${system} = with pkgs; mkShell {
        buildInputs = [
          # all of rust unstable
          rust_channel
          # fast linking
          lld
          # not sure
          pkg-config
          # for tbe web
          wasm-pack
          # web development
          nodejs
          nodePackages.yarn

          rust-analyzer
        ];

        # don't warn for dead code, unused imports or unused variables
        RUSTFLAGS = "-A dead_code -A unused_imports -A unused_variables";
      };
    };
}
