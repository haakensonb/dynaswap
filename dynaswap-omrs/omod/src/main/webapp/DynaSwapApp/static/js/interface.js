// Constants should be moved to a different file or determined by loading from
// config settings.
const BASE_URL = 'http://127.0.0.1:8000';

function getCookie(name) {
    var cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        var cookies = document.cookie.split(';');
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i].trim();
            // Does this cookie string begin with the name we want?
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}
var csrftoken = getCookie('csrftoken');

const createDAGBtn = document.querySelector("#create_dag");
createDAGBtn.addEventListener("click", (e) => {
    e.preventDefault();
    getData(`${BASE_URL}/interface/create_dag`)
    .then((res) => {
        if(res.alert){
            alert(res.alert);
        }
        // reveal box holding dag
        let cyEl = document.getElementById("cy");
        cyEl.classList.remove("is-hidden");
        cyEl.classList.add("fade-in");
        // box reveal changes dimensions so must resize
        cy.resize();
        // update the dag
        cy.json(res.formatted_graph);
        cy.layout(dagreDefaults).run();

    });
});

async function postData(url, data){
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRFToken': csrftoken
        },
        body: JSON.stringify(data)
    });
    return await response.json();
}

async function getData(url){
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    });
    return response.json();
}

// DANGEROUS global variables
// needed by multiple event listener functions to properly render graph
let cy;
let dagreDefaults;

document.addEventListener("DOMContentLoaded", ()=>{
    cy = cytoscape({
        container: document.getElementById("cy"),
    
        elements: [ // list of graph elements to start with
            { // node a
              data: { id: 'a' }
            },
            { // node b
              data: { id: 'b' }
            },
            { // edge ab
              data: { id: 'ab', source: 'a', target: 'b' }
            }
          ],
        
            style: [ // the stylesheet for the graph
                {
                selector: 'node',
                style: {
                    'background-color': '#666',
                    'label': 'data(id)'
                }
                },
            
                {
                selector: 'edge',
                style: {
                    'width': 3,
                    'line-color': '#ccc',
                    'target-arrow-color': '#ccc',
                    'target-arrow-shape': 'triangle',
                    'curve-style': 'bezier'
                    }
                }
            ],
        
            layout: {
                name: 'dagre'
            },

            // interaction options
            minZoom: 1e-1,
            maxZoom: 1e1,
            zoomingEnabled: true,
            userPanningEnabled: true,
            boxSelectionEnabled: true,
            selectionType: 'single',
            touchTapThreshold: 8,
            desktopTapThreshold: 4,
            autolock: false,
            autoungrabify: false,
            autounselectify: false,

            // rendering options:
            headless: false,
            styleEnabled: true,
            hideEdgesOnViewport: false,
            textureOnViewport: false,
            motionBlur: false,
            motionBlurOpacity: 0.2,
            wheelSensitivity: 1,
            pixelRatio: 'auto'
    });

    dagreDefaults  = {
        name: "dagre",
        // dagre algo options, uses default value on undefined
        nodeSep: undefined, // the separation between adjacent nodes in the same rank
        edgeSep: undefined, // the separation between adjacent edges in the same rank
        rankSep: undefined, // the separation between each rank in the layout
        rankDir: undefined, // 'TB' for top to bottom flow, 'LR' for left to right,
        ranker: undefined, // Type of algorithm to assign a rank to each node in the input graph. Possible values: 'network-simplex', 'tight-tree' or 'longest-path'
        minLen: function( edge ){ return 1; }, // number of ranks to keep between the source and target of the edge
        edgeWeight: function( edge ){ return 1; }, // higher weight edges are generally made shorter and straighter than lower weight edges
      
        // general layout options
        fit: true, // whether to fit to viewport
        padding: 30, // fit padding
        spacingFactor: undefined, // Applies a multiplicative factor (>0) to expand or compress the overall area that the nodes take up
        nodeDimensionsIncludeLabels: false, // whether labels should be included in determining the space used by a node
        animate: false, // whether to transition the node positions
        animateFilter: function( node, i ){ return true; }, // whether to animate specific nodes when animation is on; non-animated nodes immediately go to their final positions
        animationDuration: 500, // duration of animation in ms if enabled
        animationEasing: undefined, // easing of animation if enabled
        boundingBox: undefined, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
        transform: function( node, pos ){ return pos; }, // a function that applies a transform to the final node position
        ready: function(){}, // on layoutready
        stop: function(){} // on layoutstop
      };

      cy.layout(dagreDefaults).run();
});