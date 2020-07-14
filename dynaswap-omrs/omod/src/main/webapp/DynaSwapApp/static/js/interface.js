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
// console.log(csrftoken);

function getAndRenderPrivs(){
    getData('http://127.0.0.1:8000/interface/all_possible_privs')
    .then(data => {
        renderList(data, "possible_privs", renderPossiblePriv);
    });
}

function getAndRenderRolesAndPrivs(){
    getData('http://127.0.0.1:8000/interface/roles_and_privs')
    .then(data => {
        renderList(data, "roles", renderRoleAndPriv);
    });
}

function clearList(listEl){
    while(listEl.firstChild){
        listEl.removeChild(listEl.firstChild);
    }
}

function renderList(data, listSelector, callback){
    const listEl = document.querySelector(`#${listSelector}`);
    // make sure the list is cleared
    clearList(listEl);

    data['context'].forEach((data) => {
        callback(listEl, data);
    })
};

function renderPossiblePriv(listEl, priv, checked = false){
    console.log(priv);
    console.log(checked);
    const listItem = document.createElement('li');
    listEl.appendChild(listItem);
    const checkbox = document.createElement('input');
    checkbox.type = "checkbox";
    checkbox.checked = checked;
    checkbox.name = priv.priv_name;
    listItem.appendChild(checkbox);
    listItem.appendChild(document.createTextNode(priv.priv_name));
}

function renderRoleAndPriv(listEl, role_and_priv){
    const listItem = document.createElement('li');
    listEl.appendChild(listItem);
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.name = role_and_priv.role_name;
    listItem.appendChild(checkbox);
    listItem.appendChild(document.createTextNode(role_and_priv.role_name));
    // janky way to create linebreak, change later
    listItem.appendChild(document.createElement('br'));
    // add button for editing privs
    const editBtn = document.createElement('button');
    editBtn.value = role_and_priv.role_name;
    editBtn.innerText = `Edit ${role_and_priv.role_name} Priviliges`;
    editBtn.classList.add("edit_priv", "button", "is-info");
    editBtn.addEventListener("click", openEditBtn);
    listItem.appendChild(editBtn);
    // render privs for role
    const privList = document.createElement('ul');
    listItem.appendChild(privList);
    role_and_priv.privs.forEach(priv => {
        const privItem = document.createElement('li');
        privItem.innerText = priv;
        privList.appendChild(privItem)
    });

}

// modal event listeners
async function openEditBtn(e){
    document.querySelector(".modal").classList.add('is-active');
    const role_name = e.target.value; 
    const res = await getData(`http://127.0.0.1:8000/interface/get_role_privs/${role_name}`);
    let possible_privs = res.context.possible_privs;
    let role = res.context.role;
    const listEl = document.querySelector("#role_privs");
    clearList(listEl);
    possible_privs.forEach(priv => {
        if(role.privs.includes(priv.priv_name)){
            renderPossiblePriv(listEl, priv, true);
        } else{
            renderPossiblePriv(listEl, priv);
        }
    });
    // put role in button name so that it can be accessed again in another function
    document.querySelector("#apply_priv_edit").name = e.target.value;
}
const modalBtn = document.querySelector(".modal-close");
const modalBackground = document.querySelector(".modal-background");
const modalCloseHandler = (e) => {
    document.querySelector(".modal").classList.remove('is-active');
};
modalBtn.addEventListener("click", modalCloseHandler);
modalBackground.addEventListener("click", modalCloseHandler);

async function editBtnHandler(e){
    e.preventDefault();
    let selectedPrivs = [];
    let notSelectedPrivs = [];
    const checkboxes = document.querySelector("#role_priv_checkboxes").querySelectorAll("input[type=checkbox]");
    checkboxes.forEach(checkbox => {
        if(checkbox.checked){
            selectedPrivs.push(checkbox.name);
        } else {
            notSelectedPrivs.push(checkbox.name);
        }
    });
    const role = e.target.name;
    data = {role: role, selected_privs: selectedPrivs, not_selected_privs: notSelectedPrivs};    
    // wait for data to be updated and then rerender the roles and privs
    await postData('http://127.0.0.1:8000/interface/edit_role_priv_assignment', data);
    getAndRenderRolesAndPrivs();
    // also close the modal
    document.querySelector('.modal').classList.remove('is-active');
}
const applyPrivEditBtn = document.querySelector("#apply_priv_edit");
applyPrivEditBtn.addEventListener("click", editBtnHandler);

const addPrivFormEl = document.querySelector("#add_priv");
const addPrivHandler = async (e) => {
    e.preventDefault();
    const addPrivForm = new FormData(addPrivFormEl);
    const priv = addPrivForm.get('add_priv');
    data = {add_priv: priv}
    const res = await postData('http://127.0.0.1:8000/interface/possibleprivilege/add', data);
    console.log(res);
    if(res.context.hasOwnProperty('added_priv')){
        const listEl = document.querySelector('#possible_privs');
        console.log(res.context.added_priv);
        // render method expects object in this format
        renderPossiblePriv(listEl, {priv_name: res.context.added_priv});
    }
};
addPrivFormEl.addEventListener("submit", addPrivHandler);

// could be changed so that one function handles addPrivHandler and addRoleHandler
// but they might end up being different in the future so for now they will get their own functions
const addRoleFormEl = document.querySelector("#add_role");
const addRoleHandler = async (e) => {
    e.preventDefault();
    const addRoleForm = new FormData(addRoleFormEl);
    const role = addRoleForm.get("role");
    data = {role: role};
    const res = await postData('http://127.0.0.1:8000/interface/role/add', data);
    console.log(res);
    if(res.context.hasOwnProperty('role')){
        const listEl = document.querySelector("#roles");
        renderRoleAndPriv(listEl, res.context.role);
    }
};
addRoleFormEl.addEventListener("submit", addRoleHandler);

function toggleAllCheckboxHandler(e, checkboxSelector){
    const checkboxContainer = document.querySelector(`#${checkboxSelector}`);
    const checkboxes = checkboxContainer.querySelectorAll("input[type=checkbox]");
    const srcChecked = e.target.checked;
    checkboxes.forEach((checkbox) => {
        checkbox.checked = srcChecked;
    });
}

const togglePossiblePrivCheckbox = document.querySelector("#toggle_possible_priv_select");
togglePossiblePrivCheckbox.addEventListener("click", (e) => toggleAllCheckboxHandler(e, "possible_privs_checkboxes"));

const toggleRolesCheckbox = document.querySelector("#toggle_role_select");
toggleRolesCheckbox.addEventListener("click", (e) => toggleAllCheckboxHandler(e, "roles_checkboxes"));

const deletePrivFormEl = document.querySelector("#delete_priv");
const deletePrivHandler = async (e) => {
    e.preventDefault();
    const checkboxes = document.querySelector("#possible_privs_checkboxes").querySelectorAll("input[type=checkbox]");
    privsToDel = [];
    checkboxes.forEach((checkbox) => {
        if(checkbox.checked){
            privsToDel.push(checkbox.name);
        }
    });
    data = {delete_privs: privsToDel};
    await postData('http://127.0.0.1:8000/interface/possibleprivilege/delete', data);
    getAndRenderPrivs();
    // also need to rerender roles
    getAndRenderRolesAndPrivs();
};
deletePrivFormEl.addEventListener("submit", deletePrivHandler);

// could maybe be merged with deletePrivFormEl
const deleteRoleFormEl = document.querySelector("#delete_role");
const deleteRoleHandler = async (e) => {
    e.preventDefault();
    const checkboxes = document.querySelector("#roles_checkboxes").querySelectorAll("input[type=checkbox]");
    rolesToDel = [];
    checkboxes.forEach((checkbox) => {
        if(checkbox.checked){
            rolesToDel.push(checkbox.name);
        }
    });
    data = {roles: rolesToDel};
    await postData('http://127.0.0.1:8000/interface/role/delete', data);
    getAndRenderRolesAndPrivs();
}
deleteRoleFormEl.addEventListener("submit", deleteRoleHandler);

const createDAGBtn = document.querySelector("#create_dag");
createDAGBtn.addEventListener("click", (e) => {
    e.preventDefault();
    getData('http://127.0.0.1:8000/interface/create_dag')
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

// need buttons for priv editing

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

getAndRenderPrivs();
getAndRenderRolesAndPrivs();


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