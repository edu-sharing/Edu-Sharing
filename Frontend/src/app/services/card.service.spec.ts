import { TestBed } from '@angular/core/testing';
import { TestScheduler } from 'rxjs/testing';
import { CardService } from './card.service';

describe('CardService', () => {
    let service: CardService;
    let scheduler: TestScheduler;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CardService);
        scheduler = new TestScheduler((actual, expected) => {
            void expect(actual).toEqual(expected);
        });
    });

    it('should be created', () => {
        void expect(service).toBeTruthy();
    });

    describe('hasOpenModals', () => {
        it('should default to false', () => {
            scheduler.run(({ expectObservable }) => {
                expectObservable(service.hasOpenModals).toBe('a', { a: false });
            });
        });

        it('should set new values', () => {
            service.setNumberModalCards(23);
            service.setNumberModalCards(42);
            scheduler.run(({ expectObservable }) => {
                expectObservable(service.hasOpenModals).toBe('a', {
                    a: true,
                });
            });
        });
    });
});
