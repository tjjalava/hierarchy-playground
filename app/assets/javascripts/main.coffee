$ ->
  treeContainer = $("#tree-container")

  statusLogger = (->
    _start = 0
    start: ->
      _start = new Date().getTime()
    log: (msg) ->
      $("#status").html((msg or "") + " - " + (new Date().getTime() - _start) + " ms")
  )()

  $("#create-new").click ->
    treeContainer.jstree("create", -1)

  $("#fetch-tree").click ->
    rootId = parseInt($("#root-id").val(), 10) or -1

    if !isNaN(rootId)
      levels = parseInt($("#levels").val(), 10)
      levels = 2 if isNaN(levels) or levels <= 0
      rootList = []
      entityMap = {}
      queryRoot = -1

      dataFn = (entity) ->
        entity.data = entity.name
        entity.attr = "id": "id" + entity.id
        entity.metadata = "id": entity.id
        entity.state = "closed" if entity.hasChildren
        entity.parentId = -1 if entity.parentId == entity.id
        if (parent = entityMap[entity.parentId])?
          (parent.children ?= []).push(entity)
        else
          rootList.push(entity)
        entityMap[entity.id] = entity

      treeContainer.jstree("destroy").html("")
      treeContainer
        .on
          "move_node.jstree": (e, d) ->
            if d.rslt.cy
              alert("Copy is not supported")
              return false

            nodeId = d.rslt.o.data("id")
            targetId = d.rslt.cr.data?("id") or -1
            console.log "move_node " + nodeId + " to " + targetId
            statusLogger.start()
            $.ajax("json/" + nodeId + "/move/" + targetId, type: "PUT")
              .done ->
                console.log "Move finished"
              .fail ->
                $.jstree.rollback(d.rlbk)
              .always ->
                statusLogger.log("Move node")

          "create.jstree": (e, d) ->
            console.log "create"
            name = d.rslt.name
            parentId = d.rslt.parent.data?("id") or -1
            url = "json"
            if parentId >= 0 then url += "/" + parentId
            statusLogger.start()
            $.ajax(
              url,
              contentType: "application/json"
              type: "POST"
              data: JSON.stringify(
                name: name
                description: ""
              )
            )
            .done (data) ->
              console.log "Create succesful"
              dataFn(data)
              $(d.rslt.obj).attr("id", "id" + data.id).data(id: data.id)
            .fail () ->
                $.jstree.rollback(d.rlbk)
            .always ->
                statusLogger.log("Create node")


          "rename.jstree": (e,d) ->
            console.log "rename"
            name = d.rslt.new_name
            id = $(d.rslt.obj).data("id")
            statusLogger.start()
            $.ajax("json/" + id,
              contentType: "application/json"
              type: "PUT"
              data: JSON.stringify(
                name: name
                description: entityMap[id].description
              )
            )
              .done ->
                console.log "rename succesful"
              .fail ->
                console.log "rename failed"
                $.jstree.rollback(d.rlbk)
              .always ->
                statusLogger.log("Rename node")

          "remove.jstree": (e,d) ->
            console.log "delete"
            statusLogger.start()
            $.ajax("json/" + $(d.rslt.obj).data("id"),
              type: "DELETE"
            )
              .done ->
                console.log "delete succesful"
              .fail ->
                console.log "delete failed"
                $.jstree.rollback(d.rlbk)
              .always ->
                statusLogger.log("Delete node")

          "copy.jstree": (e,d) ->
            console.log "copy"
            e.stopImmediatePropagation()

          "cut.jstree": (e,d) ->
            console.log "cut"

          "paste.jstree": (e,d) ->
            console.log "paste"

      treeContainer.jstree {

        "crrm":
          "move":
            "check_move": (m) ->
              !m.cy and m.cr != -1

        "json_data":
          "progressive_render": true
          "ajax":
            "data": ->
              statusLogger.start()
              depth: levels

            "url": (node) ->
              id = if node == -1 then rootId else parseInt(node.data("id"), 10)
              queryRoot = id unless id == rootId
              "json/" + id

            "success": (data) ->
              dataFn(entity) for entity in data when entity.id isnt queryRoot
              statusLogger.log("Node loaded")
              if queryRoot == -1 then rootList else entityMap[queryRoot].children

        "plugins": ["themes", "ui", "json_data", "crrm", "dnd", "contextmenu" ]
      }
