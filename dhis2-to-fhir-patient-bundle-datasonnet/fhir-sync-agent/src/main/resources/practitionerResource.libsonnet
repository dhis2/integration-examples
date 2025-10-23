{
  practitioner_entry_from_user(ds, user)::
    local username = (user.username) default null;
    local firstName = (user.firstName) default null;
    local surname = (user.surname) default null;
    local uid = (user.uid) default null;

    if username == null || ds.trim(username) == "" then {}
    else {
      fullUrl: "urn:uuid:dhis2:user:" + uid,
      resource: std.prune({
        resourceType: "Practitioner",
        identifier: std.prune([
          {
            system: "urn:dhis2:user:uid",
            value: uid
          }
        ]),
        active: true,
        name: [
          {
            text: (std.join(" ", std.prune([firstName, surname]))) default username,
            given: std.prune([firstName]),
            family: surname
          }
        ]
      }),
      request: {
        method: "PUT",
        url: "Practitioner?identifier=urn:dhis2:user:uid|" + uid
      }
    }
}