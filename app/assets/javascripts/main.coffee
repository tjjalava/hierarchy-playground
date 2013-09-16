$ ->
  treeContainer = $("#tree-container")

  $("#fetch-tree").click ->
    rootId = parseInt($("#root-id").val(), 10)

    if !isNaN(rootId) and rootId >= 0
      levels = parseInt($("#levels").val(), 10)
      levels = 2 if isNaN(levels) or levels <= 0
      rootList = []
      entityMap = {}
      queryRoot = -1

      dataFn = (entity) ->
        entity.data = entity.name
        entity.attr = { "id": "id" + entity.id }
        entity.metadata = { "id": entity.id }
        entity.state = "closed" if entity.hasChildren
        if (parent = entityMap[entity.parentId])?
          (parent.children ?= []).push(entity)
        else
          rootList.push(entity)
        entityMap[entity.id] = entity


      treeContainer
        .on
          "move_node.jstree": (e, d) ->
            if d.rslt.cy
              alert("Copy is not supported")
              return false

            nodeId = d.rslt.o.data("id")
            targetId = d.rslt.cr.data?("id") or -1
            console.log "move_node " + nodeId + " to " + targetId
            $.post("json/" + nodeId + "/move/" + targetId)
              .done ->
                console.log "Move finished"
              .fail ->
                alert("Fail")

          "create.jstree": (e,d) ->
            console.log "create"
            $.ajax(
              "json",
              contentType: "application/json"
              type: "POST"
              data: JSON.stringify(
                name: "New name"
                description: "Desc"
              )
            )

          "rename.jstree": (e,d) ->
            console.log "rename"

          "delete.jstree": (e,d) ->
            console.log "delete"

          "copy.jstree": (e,d) ->
            console.log "copy"
            e.stopImmediatePropagation()

          "cut.jstree": (e,d) ->
            console.log "cut"

          "paste.jstree": (e,d) ->
            console.log "paste"

      treeContainer.jstree("destroy").html("")
      treeContainer.jstree {

        "crrm":
          "move":
            "check_move": (m) ->
              ok = !m.cy and m.cr != -1
              console.log "Ok? " + ok
              ok

        "json_data":
          "progressive_render": true
          "ajax":
            "data": ->
              depth: levels

            "url": (node) ->
              id = if node == -1 then rootId else parseInt(node.data("id"), 10)
              queryRoot = id unless id == rootId
              "json/" + id

            "success": (data) ->
              dataFn(entity) for entity in data when entity.id isnt queryRoot
              if queryRoot == -1 then rootList else entityMap[queryRoot].children


        "plugins": ["themes", "ui", "json_data", "crrm", "dnd", "contextmenu" ]
      }
