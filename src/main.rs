use tokio::io::{AsyncReadExt, Result};
use tokio::net::{TcpListener, TcpStream};

#[tokio::main]
async fn main() -> Result<()> {
    let listener_address = "127.0.0.1:8080";
    let listener = TcpListener::bind(listener_address).await?;

    println!("Server listening on {}", listener_address);

    loop {
        match listener.accept().await {
            Ok((socket, addr)) => {
                println!("Accepted connection from {}", addr);

                tokio::spawn(async move {
                    handle_connection(socket).await;
                    println!("Connection closed from: {}", addr);
                });
            }
            Err(e) => {
                eprintln!("Error accepting connection: {}", e);
            }
        }
    }
}

async fn handle_connection(mut stream: TcpStream) {
    let mut buffer = [0; 1024];

    loop {
        match stream.read(&mut buffer).await {
            Ok(0) => {
                break;
            }
            Ok(n) => {
                let message = String::from_utf8_lossy(&buffer[..n]);
                println!("Received message: {}", message);
            }
            Err(e) => {
                eprintln!("Failed to read from socket; err = {:?}", e);
                break;
            }
        }
    }
}
