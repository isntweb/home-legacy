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
      isntweb-bundle = pkgs.writeScriptBin
        # port is the second arg
        "isntweb-serve" "${isntweb-home-server}/bin/isntweb-home-server ${static-sources}/static 6200";

      isntwebHomeModule = import ./.;

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
        ];

        # don't warn for dead code, unused imports or unused variables
        RUSTFLAGS = "-A dead_code -A unused_imports -A unused_variables";
      };
    };
}
