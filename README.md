
Cytoscape 3 App: Legend-creator 
================================================

Legends are common tools to annotate a network.  Each frame in a legend corresponds to a <i>mapped attribute</i>, meaning that the display of the node or edge varies with data stored in a table.  A mapping is either discrete or continuous.

The primary type of legend is the gradient menu.  Cytoscape has recently adopted the BrewerColor system of color palettes, better matching visualizations generated in R.  

With the extend annotation capabilities in v3.7, we have rewritten the legend feature to add annotations directly to the Cytoscape canvas.  A legend explains a mapping between attributes of the data and the features of the visualization.

In practice, it is common to map multiple attributes to a single parameter.  E.g., both node size and font size commonly map together to a expression level

The Legend Creator is a Cytoscape app, and as such is installed and updated via the Cytoscape App Store.

Once the app is installed you will have an additional tab added to the Control Panel.

In this version, you must manually initiate the scan of the network to list the mapped parameters.

[TODO] automate the scan of the network when view changes

Hit the scan button to process the network, looking for mapped attributes.

Many mapped attributes are not worth showing in a legend, so you can uncheck boxes to reduce the size of the legend produced.

There are fields for title and subtitle of the legend, drawn in two lines, and an optional bounding box.  The multiple items can be drawn in a vertical or horizontal orientation.

Once you have set up these settings, click the Add Legend button.  Then click on the canvas to set the position of the top left corner of the bounding box.

Once the legend is created, you can edit the fonts, line styles, etc. through the Annotations panel, or by right-clicking on the element. 


For Developers: Legend-creator FAQ
================================================
This is a repository of a new sample app that is more comprehensive than other samples. 

As a part of building the legend, it traverses the structure of the current network, serving as a good example of how to access the network model and view.

Then it uses that information to create annotations that are inserted onto the Cytoscape canvas.

This example shows how to add a tab to the control panel, how to add a menu item that brings that tab to the foreground, and how to create annotations based on the state of the current network.
