import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
    selector: 'es-search-page-toolbar',
    templateUrl: './search-page-toolbar.component.html',
    styleUrls: ['./search-page-toolbar.component.scss'],
})
export class SearchPageToolbarComponent implements OnInit {
    @Input() filterBarIsVisible: boolean;
    @Output() filterBarIsVisibleChange = new EventEmitter<boolean>();

    constructor() {}

    ngOnInit(): void {}

    toggleFilterBar() {
        this.filterBarIsVisible = !this.filterBarIsVisible;
        this.filterBarIsVisibleChange.emit(this.filterBarIsVisible);
    }
}
