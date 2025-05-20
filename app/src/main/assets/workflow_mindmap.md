# OmerFlex App Workflow Mindmap

```
                                                +-------------------+
                                                | OmerFlexApplication |
                                                +-------------------+
                                                         |
                                                         v
                +------------------------------+------------------------+
                |                              |                        |
                v                              v                        v
    +----------------------+      +------------------------+    +----------------+
    | MobileWelcomeActivity |      | MainActivity (TV)      |    | Services      |
    +----------------------+      +------------------------+    +----------------+
                |                              |                        |
                v                              v                        |
    +----------------------+      +------------------------+            |
    | MobileHomepageActivity|      | MainFragment          |            |
    +----------------------+      +------------------------+            |
                |                              |                        |
                |                              |                        |
                |                              |                        |
                v                              v                        v
    +----------------------+      +------------------------+    +----------------+
    | MainViewControl      |<---->| SearchViewControl      |<-->| ServerManager  |
    +----------------------+      +------------------------+    +----------------+
                |                              |                        |
                |                              |                        |
                v                              v                        v
    +----------------------+      +------------------------+    +----------------+
    | Movie Categories     |      | Search Results         |    | Servers        |
    +----------------------+      +------------------------+    +----------------+
                |                              |                        |
                |                              |                        |
                v                              v                        v
    +----------------------+      +------------------------+    +----------------+
    |MobileMovieDetailActivity    | DetailsActivity        |    | AbstractServer |
    +----------------------+      +------------------------+    +----------------+
                |                              |                        |
                v                              v                        |
    +----------------------+      +------------------------+            |
    | DetailsViewControl   |<---->| VideoDetailsFragment   |<-----------+
    +----------------------+      +------------------------+
                |                              |
                |                              |
                v                              v
    +----------------------+      +------------------------+
    | ExoplayerMediaPlayer |      | PlaybackActivity       |
    +----------------------+      +------------------------+
                |                              |
                v                              v
    +----------------------+      +------------------------+
    | Movie Playback       |      | PlaybackVideoFragment  |
    +----------------------+      +------------------------+
```

## App Flow Description

1. **Application Initialization**
   - `OmerFlexApplication` initializes core components (HttpClientManager, ThreadPoolManager, DatabaseManager)

2. **Entry Points**
   - Mobile: `MobileWelcomeActivity` → `MobileHomepageActivity`
   - TV: `MainActivity` → `MainFragment`

3. **Content Loading**
   - `MainViewControl` extends `SearchViewControl` to handle content loading
   - `ServerManager` manages different content servers
   - Content is loaded from various servers (OmarServer, IptvServer, etc.)
   - Content is organized into categories and displayed in rows

4. **User Interactions**
   - User can browse content categories
   - User can search for content
   - User can select a movie/show to view details

5. **Details View**
   - Mobile: `MobileMovieDetailActivity`
   - TV: `DetailsActivity` → `VideoDetailsFragment`
   - Details include title, description, background image, actions

6. **Playback**
   - Mobile: `ExoplayerMediaPlayer`
   - TV: `PlaybackActivity` → `PlaybackVideoFragment`
   - Playback history is saved to database

7. **Background Services**
   - `UpdateService` checks for app updates
   - `DatabaseManager` handles database operations
   - `HttpClientManager` manages network requests
   - `ThreadPoolManager` manages thread pools for different operations

## Key Components

1. **UI Components**
   - Activities and Fragments for different screens
   - Adapters for displaying content
   - Presenters for formatting content

2. **Data Components**
   - `Movie` entity represents video content
   - `ServerConfig` represents server configuration
   - `MovieDbHelper` handles database operations

3. **Service Components**
   - `ServerManager` manages content servers
   - `UpdateService` handles app updates
   - `HttpClientManager` manages network requests
   - `ThreadPoolManager` manages thread pools
   - `ImageLoader` handles image loading

4. **Control Components**
   - `MainViewControl` handles main view logic
   - `SearchViewControl` handles search logic
   - `DetailsViewControl` handles details view logic