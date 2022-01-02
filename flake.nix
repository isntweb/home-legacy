{
  description = "good programming language";

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
        packages.joss = naersk-lib.buildPackage {
          pname = "joss";
          root = ./.;
        };

        apps.joss = utils.lib.mkApp {
          drv = packages.joss;
        };

        defaultPackage = packages.joss;


        devShell = with pkgs; mkShell {
          buildInputs = [
            # all of rust unstable
            rust_channel
            # fast linking
            lld
            # not sure
            pkg-config
            # sqlite3 support for journal store
            sqlite
            # for `rug`
            gnum4
            # for tbe web
            wasm-pack
            # database management
            diesel-cli
            # web development
            nodejs
            nodePackages.yarn
          ];

          # for rust-analyzer; the target dir of the compiler for the project
          OUT_DIR = "./target";
          # store for all information
          DATABASE_URL = "./journal/JOURNAL.sqlite3";
          # don't warn for dead code, unused imports or unused variables
          RUSTFLAGS = "-A dead_code -A unused_imports -A unused_variables";
          # force cross compilation when there is potential for it
          CARGO_FEATURE_FORCE_CROSS = "true";
        };
      });
}
