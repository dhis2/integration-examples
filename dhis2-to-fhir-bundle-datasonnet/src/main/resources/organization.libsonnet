{
  organization_resource(ds, orgUnit)::
    {
      resource: {
        resourceType: 'Organization',
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
        type: [
          {
            coding: [
              {
                system: 'http://terminology.hl7.org/CodeSystem/organization-type',
                code: 'prov',
                display: 'Facility',
              },
            ],
          },
        ],
        name: orgUnit.name,
      },
      request: {
        method: 'POST',
        url: 'Organization?identifier=%g' % orgUnit.id,
        ifNoneExist: 'identifier=%g' % orgUnit.id,
      },
    },
}