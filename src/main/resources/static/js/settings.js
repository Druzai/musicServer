function load(){
    checkboxScrollTo.checked = JSON.parse(localStorage.getItem('checkboxScrollTo')) ?? false;
    checkboxAutoplay.checked = JSON.parse(localStorage.getItem('checkboxAutoplay')) ?? true;
}

function save(){
    localStorage.setItem('checkboxScrollTo', checkboxScrollTo.checked);
    localStorage.setItem('checkboxAutoplay', checkboxAutoplay.checked);
}