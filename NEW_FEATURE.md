# M1

## New Feature

**Name:** Hobby News Feed

**Short description:** This new feature uses the newsapi from newsapi.org to collect news articles based on keywords, in this case based on the already existing hobbies. As hobbies are modified, the news feed on the home page is refreshed to show 5 news articles per hobby. The news articles are grouped by hobby using scrollabe cards, each containing the title, link to the article, and date of the article.

**Location and code:** On the backend, news.controller.ts and news.routes.ts. On the frontend, NewsInterface, NewsModels, NewsScreen, and NewsViewModel were added, and various files were modified to add the news feed to the main page, including MainScreen and Navigation.
