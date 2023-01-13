const form = document.getElementById('formid');
let origin = window.location.origin;

window.onload = async (e) => {
    form.reset();
    $("#uploadSpin").hide();
    await refreshFiles();
};


function generateDiv(position, filename, url, uuid, fileType) {
    let row = document.createElement("tr");
    let positionColumn = document.createElement("th");
    positionColumn.innerText = `${position + 1}`;
    positionColumn.className = "text-center align-middle";
    positionColumn.scope = "row"
    row.appendChild(positionColumn);
    let nameColumn = document.createElement("td");
    nameColumn.innerText = filename;
    nameColumn.scope = "row"
    nameColumn.className = "text-center align-middle";
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
        audio.autoplay = true;
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

async function changeText(element, newText, oldText, milliseconds, show) {
    if (show) {
        element.style.display = "block";
    }
    element.innerText = newText;
    await new Promise(resolve => setTimeout(resolve, milliseconds));
    element.innerText = oldText;
    if (show) {
        element.style.display = "none";
    }
}

function deleteDivs() {
    files.innerHTML = "";
}

async function deleteFile(uuid) {
    await fetch(new URL("api/delete/" + uuid, origin), {
        method: 'DELETE'
    });
    await refreshFiles();
}

async function refreshFiles() {
    $("#send").animate({"opacity": 0.5}, 500);
    let response = await fetch(new URL("api/getMusic", origin));
    let json = await response.json();
    deleteDivs();
    for (let i = 0; i < json.length; i++) {
        let file = json[i];
        let row = generateDiv(i, file.fileName, file.fileUrl, file.fileUUID, file.fileType);
        files.appendChild(row);
    }
    $("#send").animate({"opacity": 1}, 500);
}

form.onsubmit = async (e) => {
    e.preventDefault();
    $("#uploadSpin").show();
    const url = new URL("api/upload", origin);

    try {
        const formData = new FormData();
        formData.append("file", file.files[0], file.files[0].name);
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });
        const json = await response.json();
        console.log(json);
        if (json["errors"] != null)
            await changeText(uploadError, json["message"], "", 3000, true);
        form.reset();
        await refreshFiles();
    } catch (error) {
        alert(error);
        console.error(error);
    }
    $("#uploadSpin").hide();
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