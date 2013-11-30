#!/usr/bin/env groovy
// Converts all the recipes into a GraphML graph file for Gephi et al.
// Hops share an edge if they are used together in a recipe.
// The edges are weighted by the number of recipes the hops have in common.

import groovy.xml.MarkupBuilder

Map<String, String> hopNorm = [
        "Goldings, East Kent (whole)": "Kent Goldings",
        "Goldings, East Kent": "Kent Goldings",
        "Fuggle": "Fuggles",
        "Hallertauer (leaf)": "Hallertauer",
        "Hallertauer Tradition": "Hallertauer",
        "Hallertauer, D.": "Hallertauer",
        "Hallertaur Domestic": "Hallertauer",
        "Hallertauer, New Zealand": "Hallertauer",
        "Hallertauer Hersbrucker": "Hersbrucker",
        "Tettnang (leaf)": "Tettnang",
        "Willamette": "Williamette",
        "Cascade Leaf Hops": "Cascade",
        "Pearle": "Perle",
        "Saazer": "Saaz",
        "Columbus (Tomahawk)": "Columbus"
]

class Recipe {
    String id
    String name
    String style
    Set<String> hops = new TreeSet<>()
    Set<String> grains = new TreeSet<>()
    Set<String> yeasts = new TreeSet<>()
}

List<Recipe> recipes = []
int c = 0
new File("recipes").listFiles().each { File f ->
    if (!f.name.endsWith(".xml")) return
    new XmlParser().parse(f).each { r ->
        rec = new Recipe(id: "n" + ++c, style: r.STYLE.NAME.text())
        rec.hops.addAll(r.HOPS.HOP.NAME*.text().collect { hopNorm[it] ?: it })
        rec.grains.addAll(r.FERMENTABLES.FERMENTABLE.findAll { it.TYPE.text() == "Grain" }.NAME*.text())
        rec.yeasts.addAll(r.YEASTS.YEAST.NAME*.text())
        recipes << rec
    }
}

println "${recipes.size()} recipes"

// choose the recipe for each style that uses the largest variety of hops
recipes = recipes.groupBy { it.style }.collect { style, slist -> slist.sort { -it.hops.size() }.first() }

println "${recipes.size()} beer styles"
println "${recipes.sum(0){ it.hops.size() } / recipes.size()} hop varieties used per beer recipe (style)"

class Hop {
    String id
    String name
    Set<String> styles = new TreeSet<>()
    int edgeWeight(Hop o) { styles.intersect(o.styles).size() }
}

Map<String, Hop> hops = [:]
c = 0
recipes.each { r ->
    r.hops.each { name ->
        def hop = hops[name]
        if (hop == null) hops[name] = hop = new Hop(id: "n" + ++c, name: name)
        hop.styles << r.style
    }
}

println "${hops.size()} hops"

def w = new FileWriter("hops.graphml")
def xml = new MarkupBuilder(w)
int ec = 0
List<Hop> list = hops.values().asList()
xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
xml.graphml(xmlns: "http://graphml.graphdrawing.org/xmlns") {
    key("attr.name": "label", "attr.type": "string", for: "node", id: "label")
    key("attr.name": "weight", "attr.type": "double", for: "edge", id: "weight")
    graph(edgedefault: "undirected") {
        list.each { hop ->
            node(id: hop.id) {
                data(hop.name, key: 'label')
            }
        }
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                def a = list[i]
                def b = list[j]
                int weight = a.edgeWeight(b)
                if (weight) {
                    edge(id: 'e' + ++ec, source: a.id, target: b.id) {
                        data(weight, key: 'weight')
                    }
                }
            }
        }
    }
}
w.close()
