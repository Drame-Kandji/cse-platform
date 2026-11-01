import { factureSchema } from './factures/schemas/facture.schema'
import { expeditionSchema, jalonSchema, refusSchema } from './contrats/schemas/contrat.schema'
import { ligneSchema, profilSchema } from './entreprise/schemas/entreprise.schema'
import { INSCRIPTION_VIDE, inscriptionSchema } from './inscription/schemas/inscription.schema'
import { offreSchema } from './appels-offres/schemas/offre.schema'

const erreurs = (result: { success: boolean; error?: { issues: { path: (string | number)[] }[] } }) =>
  result.error?.issues.map((i) => i.path.join('.')) ?? []

describe('inscriptionSchema', () => {
  const valide = {
    ...INSCRIPTION_VIDE,
    raisonSociale: 'Baobab Logistique SARL',
    ninea: '008789012 2E7',
    email: 'contact@baobab.sn',
    contactNom: 'Seynabou Gueye',
    specialites: ['CONSOMMABLE' as const],
  }
  it('accepte une inscription minimale et omet les champs vides', () => {
    const result = inscriptionSchema.parse(valide)
    expect(result).toMatchObject({ pays: 'Sénégal', rccm: undefined, telephone: undefined })
  })
  it('refuse NINEA, e-mail, telephone invalides et specialites vides', () => {
    const result = inscriptionSchema.safeParse({
      ...valide,
      ninea: '12',
      email: 'x',
      telephone: 'abc',
      specialites: [],
    })
    expect(erreurs(result)).toEqual(expect.arrayContaining(['ninea', 'email', 'telephone', 'specialites']))
  })
  it('exige raison sociale, pays et contact', () => {
    const result = inscriptionSchema.safeParse({ ...valide, raisonSociale: ' ', pays: '', contactNom: '' })
    expect(erreurs(result)).toEqual(expect.arrayContaining(['raisonSociale', 'pays', 'contactNom']))
  })
})

describe('offreSchema', () => {
  it('convertit les montants (espaces toleres) et la garantie facultative', () => {
    expect(
      offreSchema.parse({
        montantPropose: '96 000 000',
        delaiLivraisonJours: '30',
        garantieMois: '',
        description: '',
      }),
    ).toEqual({
      montantPropose: 96_000_000,
      delaiLivraisonJours: 30,
      garantieMois: undefined,
      description: undefined,
    })
  })
  it('refuse montant nul, delai < 1 et garantie non entiere', () => {
    const result = offreSchema.safeParse({
      montantPropose: '0',
      delaiLivraisonJours: '0',
      garantieMois: '1,5',
      description: '',
    })
    expect(erreurs(result)).toEqual(['montantPropose', 'delaiLivraisonJours', 'garantieMois'])
  })
})

describe('factureSchema', () => {
  const base = {
    contratId: 'ct-1',
    numero: 'FA-1',
    montantHt: '1000000',
    tauxTva: '18',
    dateEmission: new Date('2026-11-01'),
    dateEcheance: new Date('2026-12-01'),
  }
  it('accepte un taux decimal a virgule', () => {
    expect(factureSchema.parse({ ...base, tauxTva: '18,5' }).tauxTva).toBe(18.5)
  })
  it('echeance anterieure et champs manquants refuses', () => {
    expect(erreurs(factureSchema.safeParse({ ...base, dateEcheance: new Date('2026-10-01') }))).toEqual([
      'dateEcheance',
    ])
    expect(erreurs(factureSchema.safeParse({ ...base, contratId: '', numero: '', tauxTva: '120' }))).toEqual(
      expect.arrayContaining(['contratId', 'numero', 'tauxTva']),
    )
  })
})

describe('schemas de contrat', () => {
  it('motif de refus obligatoire', () => {
    expect(refusSchema.safeParse({ motif: 'ok' }).success).toBe(false)
    expect(refusSchema.parse({ motif: '  Délai trop court  ' }).motif).toBe('Délai trop court')
  })
  it('expedition et jalon : date requise, pas dans le futur', () => {
    expect(expeditionSchema.safeParse({ dateExpedition: null, commentaire: '' }).success).toBe(false)
    expect(
      expeditionSchema.safeParse({ dateExpedition: new Date(Date.now() + 86_400_000), commentaire: '' })
        .success,
    ).toBe(false)
    expect(
      expeditionSchema.parse({ dateExpedition: new Date('2026-10-01'), commentaire: '' }).commentaire,
    ).toBeUndefined()
    expect(jalonSchema.safeParse({ dateReelle: new Date('2026-10-01') }).success).toBe(true)
  })
})

describe('schemas entreprise', () => {
  const profil = {
    raisonSociale: 'Sahel Engins SA',
    ninea: '005123456 2G3',
    rccm: '',
    adresse: '',
    ville: 'Dakar',
    pays: 'Sénégal',
    telephone: '+221 33 849 12 34',
    email: 'contact@sahel.sn',
    contactNom: '',
    contactFonction: '',
    specialites: ['MATERIEL_ROULANT' as const],
    certifications: 'ISO 9001\n\n ISO 14001 ',
    banque: '',
    iban: '',
  }
  it('certifications : une par ligne, coordonnees bancaires facultatives', () => {
    expect(profilSchema.parse(profil).certifications).toEqual(['ISO 9001', 'ISO 14001'])
  })
  it('banque et IBAN vont ensemble', () => {
    expect(erreurs(profilSchema.safeParse({ ...profil, iban: 'SN08' }))).toEqual(['banque', 'iban'])
    expect(
      profilSchema.safeParse({ ...profil, banque: 'BAS', iban: 'SN08SN0100152000048500003035' }).success,
    ).toBe(true)
  })
  it('ligne de catalogue', () => {
    expect(
      ligneSchema.parse({
        reference: 'PH-1',
        designation: 'Pelle',
        categorie: 'MATERIEL_ROULANT',
        prixUnitaire: '98500000',
        unite: 'unité',
        delaiLivraisonJours: '0',
      }),
    ).toMatchObject({ prixUnitaire: 98_500_000, delaiLivraisonJours: 0 })
    expect(
      erreurs(
        ligneSchema.safeParse({
          reference: '',
          designation: '',
          prixUnitaire: 'x',
          unite: '',
          delaiLivraisonJours: '',
        }),
      ),
    ).toEqual(
      expect.arrayContaining([
        'reference',
        'designation',
        'categorie',
        'prixUnitaire',
        'unite',
        'delaiLivraisonJours',
      ]),
    )
  })
})
