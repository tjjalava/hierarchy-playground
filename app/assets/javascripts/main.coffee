$ ->
  $.getJSON("json/1909").done (data) ->
    rootList = []
    entityMap = {}
    ((entity) ->
      entityMap[entity.parentId]?.children.push(entity) or rootList.push(entity)
      entityMap[entity.id] = entity
      undefined
    )(entity) for entity in data

