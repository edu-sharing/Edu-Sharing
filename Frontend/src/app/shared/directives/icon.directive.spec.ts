import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({
    template: `
        <i icon="foo"></i>
        <i icon="foo" [aria]="true"></i>
        <i icon="foo" [aria]="false"></i>
        <i icon="foo" aria-label="bar"></i>
    `,
})
class TestComponent {}

describe('IconDirective', () => {
    let fixture: ComponentFixture<TestComponent>;
    let elements: DebugElement[];

    beforeEach(() => {
        fixture = TestBed.configureTestingModule({
            // declarations: [TestComponent, IconDirective],
            declarations: [TestComponent],
            // providers: [
            //     ElementRef,
            //     {
            //         provide: TranslateService,
            //         useValue: { get: (str: string) => of(str) },
            //     },
            //     {
            //         provide: ConfigurationService,
            //         useValue: { get: () => of([]) },
            //     },
            // ],
            // schemas: [NO_ERRORS_SCHEMA],
        }).createComponent(TestComponent);
        fixture.detectChanges(); // initial binding
        // elements = fixture.debugElement.queryAll(By.directive(IconDirective));
        elements = fixture.debugElement.queryAll(By.css('i'));
    });

    // FIXME: this works on a fresh angular 9 installation but crashes here.
    xit('should have four icon elements', () => {
        void expect(elements.length).toBe(4);
    });
});
