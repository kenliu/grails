package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Jeff Brown
 */
class NamedCriteriaTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''

class Publication {
   Long id
   Long version
   String title
   Date datePublished
   Boolean paperback = true

   static namedQueries = {
       recentPublications {
           def now = new Date()
           gt 'datePublished', now - 365
       }

       publicationsWithBookInTitle {
           like 'title', '%Book%'
       }

       recentPublicationsByTitle { title ->
           def now = new Date()
           gt 'datePublished', now - 365
           eq 'title', title
       }

       latestBooks {
 			maxResults(10)
 			order("datePublished", "desc")
       }

       publishedBetween { start, end ->
           between 'datePublished', start, end
       }
   }
}
''')
    }

    void testList() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)

        session.clear()

        def publications = publicationClass.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title
    }

    void testFindAllBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        3.times {
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 10).save(flush: true)
            assert publicationClass.newInstance(title: "Some Other Book",
                    datePublished: now - 10).save(flush: true)
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 900).save(flush: true)
        }
        session.clear()

        def publications = publicationClass.recentPublications.findAllByTitle('Some Book')

        assertEquals 3, publications?.size()
        assertEquals 'Some Book', publications[0].title
        assertEquals 'Some Book', publications[1].title
        assertEquals 'Some Book', publications[2].title
    }

    void testFindAllByBoolean() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save(flush: true)
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save(flush: true)
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save(flush: true)
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save(flush: true)

        def publications = publicationClass.recentPublications.findAllPaperbackByTitle('Some Book')

        assertEquals 2, publications?.size()
        assertEquals publications[0].title, 'Some Book'
        assertEquals publications[1].title, 'Some Book'
    }

    void testFindByBoolean() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save(flush: true)
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save(flush: true)
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save(flush: true)
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save(flush: true)

        def publication = publicationClass.recentPublications.findPaperbackByTitle('Some Book')

        assertEquals publication.title, 'Some Book'
    }

    void testFindBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 900).save(flush: true)
        def recentBookId = publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 10).save(flush: true).id
        session.clear()

        def publication = publicationClass.recentPublications.findByTitle('Some Book')

        assertEquals recentBookId, publication.id
    }

    void testCountBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        3.times {
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 10).save(flush: true)
            assert publicationClass.newInstance(title: "Some Other Book",
                    datePublished: now - 10).save(flush: true)
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 900).save(flush: true)
        }
        session.clear()

        def numberOfNewBooksNamedSomeBook = publicationClass.recentPublications.countByTitle('Some Book')

        assertEquals 3, numberOfNewBooksNamedSomeBook
    }

    void testListOrderBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert publicationClass.newInstance(title: "Book 1", datePublished: now).save(flush: true)
        assert publicationClass.newInstance(title: "Book 5", datePublished: now).save(flush: true)
        assert publicationClass.newInstance(title: "Book 3", datePublished: now - 900).save(flush: true)
        assert publicationClass.newInstance(title: "Book 2", datePublished: now - 900).save(flush: true)
        assert publicationClass.newInstance(title: "Book 4", datePublished: now).save(flush: true)
        session.clear()

        def publications = publicationClass.recentPublications.listOrderByTitle()

        assertEquals 3, publications?.size()
        assertEquals 'Book 1', publications[0].title
        assertEquals 'Book 4', publications[1].title
        assertEquals 'Book 5', publications[2].title

    }

    void testGetWithIdOfObjectWhichDoesNotMatchCriteria() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def hasBookInTitle = publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10).save(flush: true)
        assert hasBookInTitle
        def doesNotHaveBookInTitle = publicationClass.newInstance(title: "Some Publication",
                datePublished: now - 900).save(flush: true)
        assert doesNotHaveBookInTitle

        session.clear()

        def result = publicationClass.publicationsWithBookInTitle.get(doesNotHaveBookInTitle.id)
        assertNull result
    }

    void testGetReturnsCorrectObject() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)
        assert oldPublication

        session.clear()

        def publication = publicationClass.recentPublications.get(newPublication.id)
        assert publication
        assertEquals 'Some New Book', publication.title
    }

    void testGetReturnsNull() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)
        assert oldPublication

        session.clear()

        def publication = publicationClass.recentPublications.get(42 + oldPublication.id)

        assert !publication
    }

    void testCount() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)
        assert oldPublication

        session.clear()
        assertEquals 2, publicationClass.publicationsWithBookInTitle.count()
        assertEquals 1, publicationClass.recentPublications.count()
    }

    void testCountWithParameterizedNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10).save(flush: true)
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10).save(flush: true)
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 900).save(flush: true)

        session.clear()
        assertEquals 2, publicationClass.recentPublicationsByTitle('Some Book').count()
    }

    void testMaxParam() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        (1..25).each {num ->
            publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: new Date()).save(flush: true)
        }

        def pubs = publicationClass.recentPublications.list(max: 10)
        assertEquals 10, pubs?.size()
    }

    void testMaxResults() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        (1..25).each {num ->
            publicationClass.newInstance(title: 'Book Title',
                    datePublished: new Date() + num).save(flush: true)
        }

        def pubs = publicationClass.latestBooks.list()
        assertEquals 10, pubs?.size()
    }

    void testMaxAndOffsetParam() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        (1..25).each {num ->
            publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: new Date()).save(flush: true)
        }

        def pubs = publicationClass.recentPublications.list(max: 10, offset: 5)
        assertEquals 10, pubs?.size()

        (6..15).each {num ->
            assert pubs.find { it.title == "Book Number ${num}" }
        }
    }

    void testFindAllWhereWithNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert publicationClass.newInstance(title: "Book Number ${num}",
                        datePublished: now).save(flush: true)
            }
        }

        def pubs = publicationClass.recentPublications.findAllWhere(title: 'Book Number 2')
        assertEquals 3, pubs?.size()
    }

    void testGetWithParameterizedNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def recentPub = publicationClass.newInstance(title: "Some Title",
                            datePublished: now).save(flush: true)
        def oldPub = publicationClass.newInstance(title: "Some Title",
                            datePublished: now - 900).save(flush: true)

        def pub = publicationClass.recentPublicationsByTitle('Some Title').get(oldPub.id)
        session.clear()
        assertNull pub
        pub = publicationClass.recentPublicationsByTitle('Some Title').get(recentPub.id)
        assertEquals recentPub.id, pub?.id
    }

    void testNamedQueryWithOneParameter() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert publicationClass.newInstance(title: "Book Number ${num}",
                        datePublished: now).save(flush: true)
            }
        }

        def pubs = publicationClass.recentPublicationsByTitle('Book Number 2').list()
        assertEquals 3, pubs?.size()
    }

    void testNamedQueryWithMultipleParameters() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            assert publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: ++now).save(flush: true)
        }

        def pubs = publicationClass.publishedBetween(now-2, now).list()
        assertEquals 3, pubs?.size()
    }

    void testNamedQueryWithMultipleParametersAndDynamicFinder() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            assert publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: now + num).save(flush: true)
            assert publicationClass.newInstance(title: "Another Book Number ${num}",
                    datePublished: now + num).save(flush: true)
        }

        def pubs = publicationClass.publishedBetween(now, now + 2).findAllByTitleLike('Book%')
        assertEquals 2, pubs?.size()
    }

    void testNamedQueryWithMultipleParametersAndMap() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..10).each {num ->
            assert publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: ++now).save(flush: true)
        }

        def pubs = publicationClass.publishedBetween(now-8, now-2).list(offset:2, max: 4)
        assertEquals 4, pubs?.size()
    }

    void testFindWhereWithNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert publicationClass.newInstance(title: "Book Number ${num}",
                        datePublished: now).save(flush: true)
            }
        }

        def pub = publicationClass.recentPublications.findWhere(title: 'Book Number 2')
        assertEquals 'Book Number 2', pub.title
    }
}