# Freyja Bot
## Introduction

I made this bot during my 2nd year of computer science studies on my free time. 
The goal was to create a Discord bot capable of managing a project and providing entertainment features.
This project was made using [JDA (Java Discord API)](https://jda.wiki/introduction/jda/).
I'll try to maintain this code as much as I can and add more functionalities. \
Note that English isn't my first language so the sidenotes in the code, as well as this [readme](https://github.com/Swotaa/freyjabot/blob/master/README.md) can be misspelled or badly written. \
If you want to suggest a correction or just chat with me, you can add me on discord at **swotaa**.

## Functionalities

- Test the bot using **/ping** command (Should reply with 🏓Pong!)
- Create a scheduled event using the **/event** command (Must give Name and Date, can give Description, Location and Duration)
- Reminders for the events are created along with the event (created with the **/event** command)
- Register a user in the database using the **/register** command (not used atm)
- Properly cancel and event using the **/cancelevent** command (Cancel from Discord AND delete from database so the reminders are deleted as well)
- Show members count using **/members** command

## Examples

### I'll add this part soon...
#### But in the meantime here are some text explanations :
In this part **Bold** options are required, _Italic_ ones are optional. "->" Is what the command return.
- /ping -> 🏓Pong!
- /event <b>name:</b>Test <b>date:</b>01/01/2042 08:30 <i>description:</i>This is a description <i>location:</i>France <i>duration:</i>5 \
&nbsp;&nbsp;&nbsp;&nbsp; => Date : Date format should be DD/MM/YYYY HH:mm using 24H format \
&nbsp;&nbsp;&nbsp;&nbsp; => Duration : Duration is in hours
- Reminders are set for 7 days, 3 days, 2 days and 1 day before the event
- /register will add you to the database unless you're already in it
- /cancelevent <b>event_id:</b>1432536015326289073 (You can find the **event_id** in the creation message)
- /members -> There are **nb_members** members in this server.


## Configuration

Create a `.env` file: (see [.env.example](https://github.com/Swotaa/freyjabot/blob/.env.example))
```env
DISCORD_TOKEN=your_discord_token
DB_URL=jdbc:sqlite:database.db
```
