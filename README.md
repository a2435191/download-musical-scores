# download-musical-scores

This bot scrapes data from (now archived, never forgotten) r/MusicalTheaterScores and r/MusicalScores. 
Specifically, it looks for links to file-sharing services in posts and downloads them. So far, the implemented interfaces are
* Dropbox (WIP)
* Google Drive
* Mega
* Sendspace (doesn't work without paying)
* Stack Storage
* WeTransfer

Then the scores are all batch-downloaded to a directory specified by the user. It also includes an API for more complicated tasks.

## TODO

* Complete more download interfaces
* Keep fixing bugs
* Upload directly to Google Drive/Dropbox instead of to local storage
* Write a CLI
* Create a GUI
