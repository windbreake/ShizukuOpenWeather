use clap::{Parser, Subcommand};

#[derive(Debug, Parser)]
#[command(name = "weather", version, about = "Shizuku Open Weather CLI")]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Debug, Subcommand)]
enum Command {
    Search { query: String },
    Summary { location: String },
    Current { location: String },
    Tui,
    Serve,
    Open { location: Option<String> },
    Launch { location: Option<String> },
}

fn main() {
    let cli = Cli::parse();
    println!("Shizuku Open Weather CLI {:?}", cli.command);
}
