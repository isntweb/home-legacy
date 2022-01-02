{
  description = "isntweb super mega website";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
    utils.url = "github:numtide/flake-utils";
    rust-overlay.url = "github:oxalica/rust-overlay";
    naersk.url = "github:nix-community/naersk";
  };

  outputs = { self, nixpkgs, rust-overlay, utils, naersk }:
    utils.lib.eachDefaultSystem (system:
      let
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

      in rec {
        packages.home = naersk-lib.buildPackage {
          pname = "home";
          root = ./.;
        };

        apps.joss = utils.lib.mkApp {
          drv = packages.home;
        };

        defaultPackage = packages.home;


        devShell = with pkgs; mkShell {
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
      });
}
