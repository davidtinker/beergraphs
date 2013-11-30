#!/usr/bin/env groovy
// Downloads all beer recipes from http://www.brew-monkey.com/ and stores them in recipes folder as beer XML files

import java.util.regex.Pattern

def html = new URL("http://www.brew-monkey.com/recipes/allrecipes.php").text
new File("recipes").mkdir()
def m = Pattern.compile("(?i)http://www.brew-monkey.com/recipes/beerxml/[a-z0-9]+.xml").matcher(html)
int c = 0
while (m.find()) {
    def link = html.substring(m.start(), m.end())
    try {
        new File("recipes/${link.substring(link.lastIndexOf('/') + 1)}").write(new URL(link).text)
        println "Downloaded ${link}"
        ++c
    } catch (FileNotFoundException ignore) {
        println "Not found: ${link}"
    }
}
println "Downloaded ${c} recipes"
