{
  location_resource(ds, orgUnit)::
    {
      resource: {
        resourceType: 'Location',
        id: orgUnit.id,
        identifier: [
          {
            system: 'https://play.dhis2.org/dev/api/organisationUnits',
            value: orgUnit.id,
          },
        ] + (
          if !ds.isEmpty(orgUnit.code) then [
            {
              system: 'https://play.dhis2.org/dev/api/organisationUnits',
              value: orgUnit.code,
            },
          ]
        ),
        status: 'active',
        name: orgUnit.name,
        mode: 'instance',
        type: [
          {
            text: 'OF',
          },
        ],
        physicalType: {
          coding: [
            {
              system: 'http://terminology.hl7.org/CodeSystem/location-physical-type',
              code: 'si',
            },
          ],
        },
        managingOrganization: {
          reference: 'Organization/%g' % orgUnit.id,
        },
        [if std.objectHas(orgUnit, 'parent') then 'partOf']:
          {
            reference: 'Location/%s' % orgUnit.parent.id,
          },
      },
      request: {
        method: 'POST',
        url: 'Location?identifier=%g' % orgUnit.id,
        ifNoneExist: 'identifier=%g' % orgUnit.id,
      },
    },
}