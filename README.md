# download-musical-scores

This bot scrapes data from (now archived, never forgotten) r/MusicalTheaterScores and r/MusicalScores. 
Specifically, it looks for links to file-sharing services in posts and downloads them. So far, the implemented interfaces are
* Dropbox
* Google Drive
* Mega
* Sendspace (doesn't work without paying)
* Stack Storage
* WeTransfer

See [this](src/main/java/com/github/a2435191/download_musical_scores/downloaders/implementations/interface_hist.md) for more information.

Then the scores are all batch-downloaded to a directory specified by the user. It also includes an API for more complicated tasks.

## TODO

* Complete more download interfaces
* Keep fixing bugs
* Unit tests
* Upload directly to Google Drive/Dropbox instead of to local storage
* Write a CLI
* Create a GUI
* Add more documentation to Java code
