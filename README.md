# Freyja Bot
## Introduction

I made this bot during my 2nd year of computer science studies on my free time. 
The goal was to create a Discord bot capable of managing a project and providing entertainment features.
This project was made using [JDA (Java Discord API)](https://jda.wiki/introduction/jda/).
I'll try to maintain this code as much as I can and add more functionalities. \
Note that English isn't my first language so the sidenotes in the code, as well as this [readme](https://github.com/Swotaa/freyjabot/blob/master/README.md) can be misspelled or badly written. \
If you want to suggest a correction or just chat with me, you can reach me on discord : **swotaa**.

## Heads Up

Since I'm French, there is some things that need to be changed to be "international friendly". \
I'm still studying and that side project is a way to help me manage a school project, so I can't take too much time to internationalise my bot. \
This doesn't mean that I won't, this means that it is for later (but it'll eventually come dw).

## Features

- Test the bot using **/ping** command (Should reply with 🏓Pong!)
- Create a scheduled event using the **/event** command (Must give Name and Date, can give Description, Location and Duration)
- Reminders for the events are created along with the event (created with the **/event** command)
- Register a user in the database using the **/register** command (not used atm)
- Properly cancel and event using the **/cancelevent** command (Cancel from Discord AND delete from database so the reminders are deleted as well)
- Show members count using **/members** command

## Examples

### I'll add this part soon...
#### But in the meantime here are some text explanations :
In this part **Bold** options are required, _Italic_ ones are optional. "→" Is what the command return.
- /ping → 🏓Pong!
- /event <b>name:</b>Test <b>date:</b>01/01/2042 08:30 <i>description:</i>This is a description <i>location:</i>France <i>duration:</i>5 \
&nbsp;&nbsp;&nbsp;&nbsp; => Date : Date format should be DD/MM/YYYY HH:mm using 24H format \
&nbsp;&nbsp;&nbsp;&nbsp; => Duration : Duration is in hours
- Reminders are set for 7 days, 3 days, 2 days and 1 day before the event
- /register will add you to the database unless you're already in it
- /cancelevent <b>event_id:</b>1432536015326289073 (You can find the **event_id** in the creation message)
- /members → There are **nb_members** members in this server.


## Configuration

### 1. The .env file
Rename the `.env.example`file: (see [.env.example](https://github.com/Swotaa/freyjabot/blob/.env.example))

You can find a discord bot token on the [Discord Developer Portal](https://discord.com/developers/home).

### 2. The config.json file
Rename the `config.json.example` file.
To get the discord_id, you can go on discord and right click on the user. By doing so, you will be able to copy the User ID which is the one we need.
For the rest, it's up to you. The bot need at least 3 columns per user : time, duration and description.
In the `config.json.example`, I reserved 5 columns per user because I needed a "total" which is the sum of each duration and one blank column so it's not visually too heavy.

### 3. The credentials.json
You don't have to create or edit this file, you have to get it from Google Sheets API. If you don't know how to do it, just ask an AI it will most likely help you to get it.

### 4. The discord configuration
The bot uses some hardcoded values for the listeners or the events.
You don't have much to do, you only need a `events` channel so you get the reminders and a `activite` one so Freyja listens and counts your hours.

### 5. The final step
To get a usable snapshot you'll need 2 commands:
1. `mvn clean package`
2. `java -jar ./target/FreyjaBot-1.0-SNAPSHOT.jar`