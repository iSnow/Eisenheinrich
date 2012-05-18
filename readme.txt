To build Eisenheinrich, you need to 
- check out aFileChooser (https://github.com/ipaulpro/afilechooser/) and add as library project in "Configure Build Path"
- grab a copy of the "Juice" font (http://www.fontsquirrel.com/fonts/Juice) and add juicebold.ttf and juiceregular.ttf into the folder assets/fonts/
- download "Apache Commons Lang 3" (https://commons.apache.org/lang/download_lang.cgi)
- download "Apache Commons HTTPMime 4.1" (Download and unzip httpcomponents-client-4.1.3 from https://archive.apache.org/dist/httpcomponents/httpclient/binary/ and find httpmime-4.1.3.jar in the "libs" directory)
- download jSoup 1.6 (http://jsoup.org/download)

then add commons-lang3-3.0.1.jar, httpmime-4.1.3.jar and jsoup-1.6.1.jar to the "libs" directory of Eisenheinrich. Do not add them to the build path as you would with a regular Java project - the android build tools find them if they reside in libs/

- Add "android-support-v4.jar" to the "libs" directory of Eisenheinrich