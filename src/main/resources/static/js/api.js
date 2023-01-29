const form = document.getElementById('formid');
const origin = window.location.origin;
var filesList = [];
var searchQuery = "";

searchBox.onkeyup = function () {
    if (searchBox.value === searchQuery)
        return;
    // Show all if searchBox.value < searchQuery
    if (searchBox.value.length < searchQuery.length)
        filesList.forEach(n => n.hidden = false);
    searchQuery = searchBox.value.toLowerCase();
    clearA.hidden = searchBox.value.length === 0
    // Hide not matching
    filesList
        .filter(n => !n.hidden)
        .filter(n => n.querySelector(".file-name").textContent.toLowerCase().search(searchQuery) === -1)
        .forEach(n => n.hidden = true);
};

clearA.onclick = function () {
    searchBox.value = "";
    searchBox.onkeyup();
}

window.onload = async (e) => {
    form.reset();
    formUrl.reset();
    searchBox.value = "";
    clearA.hidden = searchBox.value.length === 0
    $("#uploadSpin").hide();
    $(uploadUrlSpin).hide();
    await refreshFiles();
};


function generateRow(position, filename, url, uuid, fileType) {
    let row = document.createElement("tr");
    let positionColumn = document.createElement("th");
    positionColumn.innerText = `${position + 1}`;
    positionColumn.className = "text-center align-middle";
    positionColumn.scope = "row"
    row.appendChild(positionColumn);
    let nameColumn = document.createElement("td");
    nameColumn.innerText = filename;
    nameColumn.scope = "row"
    nameColumn.className = "text-center align-middle file-name";
    row.appendChild(nameColumn);
    let linkColumn = document.createElement("td");
    let link = document.createElement("a");
    link.href = url;
    link.innerText = url;
    linkColumn.className = "text-center align-middle";
    linkColumn.scope = "row"
    linkColumn.appendChild(link);
    row.appendChild(linkColumn);
    let actionsColumn = document.createElement("td");
    actionsColumn.style.display = "flex";
    actionsColumn.style.flexDirection = "column";
    actionsColumn.style.alignItems = "center";
    actionsColumn.scope = "row"
    let copyButton = document.createElement("button");
    copyButton.className = "btn btn-primary btn-small m-1";
    copyButton.style.borderColor = "#8a0093";
    copyButton.style.backgroundColor = "#9300c5";
    copyButton.innerText = "Скопировать";
    copyButton.onclick = async () => {
        copyTextToClipboard(url);
        await changeText(copyButton, "Скопировано!", "Скопировать", 2000, false);
    };
    actionsColumn.appendChild(copyButton);

    // let deleteButton = document.createElement("button");
    // deleteButton.className = "btn btn-primary btn-small";
    // deleteButton.style.borderColor = "#fc2020";
    // deleteButton.style.backgroundColor = "#ff0000";
    // deleteButton.style.marginLeft = "10px";
    // deleteButton.innerText = "Удалить";
    // deleteButton.onclick = () => deleteFile(uuid);
    // actionsColumn.appendChild(deleteButton);

    let playButton = document.createElement("button");
    playButton.className = "btn btn-primary btn-small m-1";
    playButton.style.borderColor = "#835a5a";
    playButton.style.backgroundColor = "#572929";
    playButton.innerText = "Воспроизвести";
    playButton.onclick = () => {
        let audio = document.createElement("audio");
        audio.className = "m-1";
        audio.controls = true;
        audio.autoplay = JSON.parse(localStorage.getItem('checkboxAutoplay')) ?? true
        actionsColumn.appendChild(audio);
        let source = document.createElement("source");
        source.src = url;
        source.type = fileType;
        audio.appendChild(source);
        actionsColumn.removeChild(playButton);
    };
    actionsColumn.appendChild(playButton);
    row.appendChild(actionsColumn);
    return row;
}

async function changeText(element, newText, oldText, milliseconds, show, color) {
    if (show) {
        element.style.display = "block";
    }
    element.style.color = color;
    element.innerText = newText;
    await new Promise(resolve => setTimeout(resolve, milliseconds));
    element.innerText = oldText;
    if (show) {
        element.style.display = "none";
    }
}

function deleteDivs() {
    files.innerHTML = "";
    filesList.length = 0;
}

async function deleteFile(uuid) {
    await fetch(new URL("api/delete/" + uuid, origin), {
        method: 'DELETE'
    });
    await refreshFiles();
}

async function refreshFiles() {
    $("#send").animate({"opacity": 0.5}, 300);
    let response = await fetch(new URL("api/getMusic", origin));
    let json = await response.json();
    deleteDivs();
    for (let i = 0; i < json.length; i++) {
        let file = json[i];
        let row = generateRow(i, file.fileName, file.fileUrl, file.fileUUID, file.fileType);
        filesList.push(row);
        files.appendChild(row);
    }
    $("#send").animate({"opacity": 1}, 300);
}

async function scrollToNewFile(filename, milliseconds) {
    if (JSON.parse(localStorage.getItem('checkboxScrollTo')) ?? false) {
        await new Promise(resolve => setTimeout(resolve, milliseconds));
        const elem = [...document.querySelectorAll("td.file-name")].filter(e => e.innerText === filename).pop()?.parentNode
        await smoothScroll(elem);
        $(elem).animate({backgroundColor: "#ffd700"}, 500);
        await new Promise(resolve => setTimeout(resolve, 500));
        $(elem).animate({backgroundColor: "#ffffff"}, 500);
    }
}

function smoothScroll(elem, options) {
    return new Promise((resolve) => {
        if (!(elem instanceof Element)) {
            throw new TypeError('Argument 1 must be an Element');
        }
        let same = 0; // a counter
        let lastPos = null; // last known Y position
        // pass the user defined options along with our default
        const scrollOptions = Object.assign({behavior: 'smooth'}, options);

        // let's begin
        elem.scrollIntoView(scrollOptions);
        requestAnimationFrame(check);

        // this function will be called every painting frame
        // for the duration of the smooth scroll operation
        function check() {
            // check our current position
            const newPos = elem.getBoundingClientRect().top;

            if (newPos === lastPos) { // same as previous
                if (same++ > 2) { // if it's more than two frames
                    /* @todo: verify it succeeded
                     * if(isAtCorrectPosition(elem, options) {
                     *   resolve();
                     * } else {
                     *   reject();
                     * }
                     * return;
                     */
                    return resolve(); // we've come to an halt
                }
            } else {
                same = 0; // reset our counter
                lastPos = newPos; // remember our current position
            }
            // check again next painting frame
            requestAnimationFrame(check);
        }
    });
}

form.onsubmit = async (e) => {
    e.preventDefault();
    $("#uploadSpin").show();
    const url = new URL("api/upload", origin);

    try {
        if (file.files[0].size / 1024 / 1024 > 100) {
            throw new Error(`Размер файла слишком большой - ${Math.floor(file.files[0].size / (1024 * 1024))} Мб!`)
        }

        const formData = new FormData();
        formData.append("file", file.files[0], file.files[0].name);
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });
        const json = await response.json();
        console.log(json);
        if (json["errors"] != null)
            changeText(uploadResult, json["message"] + "\n" + json["errors"].join("\n"), "", 3000, true, "crimson");
        else
            changeText(uploadResult, `Файл ${json["fileName"]} был загружен!` + (json["fileTranscoded"] ? "\nФайл был перекодирован в 'mp3'!" : ""), "", 3000, true, "green");
        form.reset();
        await refreshFiles();
        scrollToNewFile(json["fileName"], 1000)
    } catch (error) {
        changeText(uploadResult, error, "", 3000, true, "crimson");
        console.error(error);
    }
    $("#uploadSpin").hide();
}

formUrl.onsubmit = async (e) => {
    e.preventDefault();
    $(uploadUrlSpin).show();
    const url = new URL("api/upload/youtube", origin);

    try {
        if (inputUrl.value.indexOf("list=") !== -1)
            changeText(uploadResult, "Только одно видео из плейлиста будет загружено!", "", 3000, true, "green");
        const formData = new URLSearchParams();
        formData.set("url", inputUrl.value);
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });
        const json = await response.json();
        console.log(json);
        if (json["errors"] != null)
            changeText(uploadResult, json["message"] + "\n" + json["errors"].join("\n"), "", 3000, true, "crimson");
        else
            changeText(uploadResult, `Звук из видео ${json["fileName"]} был загружен!`, "", 3000, true, "green");
        formUrl.reset();
        await refreshFiles();
        scrollToNewFile(json["fileName"], 1000)
    } catch (error) {
        changeText(uploadResult, error, "", 3000, true, "crimson");
        console.error(error);
    }
    $(uploadUrlSpin).hide();
}

function fallbackCopyTextToClipboard(text) {
    const textArea = document.createElement("textarea");
    textArea.value = text;

    // Avoid scrolling to bottom
    textArea.style.top = "0";
    textArea.style.left = "0";
    textArea.style.position = "fixed";

    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        const successful = document.execCommand('copy');
        const msg = successful ? 'successful' : 'unsuccessful';
        console.log('Fallback: Copying text command was ' + msg);
    } catch (err) {
        console.error('Fallback: Oops, unable to copy', err);
    }

    document.body.removeChild(textArea);
}

function copyTextToClipboard(text) {
    if (!navigator.clipboard) {
        fallbackCopyTextToClipboard(text);
        return;
    }
    navigator.clipboard.writeText(text).then(function () {
        console.log('Async: Copying to clipboard was successful!');
    }, function (err) {
        console.error('Async: Could not copy text: ', err);
    });
}