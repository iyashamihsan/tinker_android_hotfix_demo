# Tinker_android_hotfix Example
Android Hotfix demo app using Tinker Hotfix solution. we can fix bug, update dex, library and resources without reinstall apk.
Based on config api call
then download patch file from backend server and then apply the patch.

Config mock API response model:
Mock API by using https://www.mockable.io/
![](config_response.PNG)

Before case- when app has a bug, null exception.

![](20191110_025058.gif)

After Hot-fix Patch Applied - Bug Fixed without re-install apk

![](20191110_024810.gif)  



# Libraries Used  
[Tinker Hot-fix solution](https://github.com/Tencent/tinker)  
[Retrofit](https://github.com/square/retrofit)  
[PR-Downloader](https://github.com/MindorksOpenSource/PRDownloader)
