$ ->
  treeContainer = $("#tree-container")

  $("#fetch-tree").click ->
    rootId = parseInt($("#root-id").val(), 10)

    if !isNaN(rootId) and rootId >= 0
      levels = parseInt($("#levels").val(), 10)
      levels = 2 if isNaN(levels) or levels <= 0
      rootList = []
      entityMap = {}
      baseLevel = 1
      queryRoot = -1

      dataFn = (entity) ->
        entity.data = entity.name
        entity.attr = { "id": "id" + entity.id }
        entity.metadata = { "id": entity.id }
        entity.children = [] if entity.hasChildren
        if (parent = entityMap[entity.parentId])?
          parent.children.push(entity)
        else
          rootList.push(entity)
        entityMap[entity.id] = entity

      treeContainer.jstree("destroy").html("")
      treeContainer.jstree {
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


        "plugins": ["themes", "ui", "json_data" ]
      }
