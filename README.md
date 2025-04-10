# Deft Controller

This project aims to integrate console, phone, and computer into one system for game control. The dream is to:
- Allow extension consoles (such as the Gamesir G8 Galileo or Gamesir X2 Lite) that attach on phones to act as console controllers for a computer client.
- Mirror game content onto phone screen.
- Display overlays over the game content on the phone screen.
- Controller output given to phone is then given to the computer client to control the game.

For example, imagine playing Eurotruck Simulator, but the map is too small for you. This allows you to cast the in-game map onto your phone while using the phone as a controller, while the main content is still displayed on laptop.

This uses bluetooth to discover nearby devices and to communicate IP addresses. Then it uses the internet to stream video onto the phone and bluetooth to communicate gamepad inputs.

This repository contains both the android application (in Kotlin) and the desktop client (in Rust)

Still working on it!