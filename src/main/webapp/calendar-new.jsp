<!DOCTYPE html>
<html>
<head>
    <title>Calendar Graph</title>
    <meta charset="UTF-8">
    <link rel="stylesheet" type="text/css" href="css/solrwayback.css">
    <script>
        // Hand over configuration from JSP to Javascript
        window.solrWaybackConfig = {};
        window.solrWaybackConfig.url = "<%= request.getParameter("url") %>";
    </script>
</head>

<body>
    <div id="wrapper">
        <div id="app">
            <harvest-title :url="url"></harvest-title>
            <harvest-date :url="url"></harvest-date>
        </div>
    </div>
    <script src="js/calendar-widget/dist/bundle.js"></script>
</body>
</html>
