function handles = OmeroLoader(handles)

% OmeroLoaderModule:
% Category: File Processing
%
% SHORT DESCRIPTION:
% Load all images in the dataset.
% *************************************************************************
%
% OmeroLoader Help
% 
%

% CellProfiler is distributed under the GNU General Public License.
% See the accompanying file LICENSE for details.
%
%
%
% Website: http://www.cellprofiler.org
%
% $Revision: 4905 $
% OmeroLoader 
% Author:
%   Donald MacDonald (Donald@lifesci.dundee.ac.uk)
% OpenMicroscopy Environment (OME)
% www.openmicroscopy.org.uk
% University of Dundee


%%%%%%%%%%%%%%%%%
%%% VARIABLES %%%
%%%%%%%%%%%%%%%%%
drawnow

[CurrentModule, CurrentModuleNum, ModuleName] = CPwhichmodule(handles);

%textVAR01 = Which Dataset do you wish to load from?
%defaultVAR01 = 1
DatasetID = char(handles.Settings.VariableValues{CurrentModuleNum,1});

%textVAR02 = Enter Username?
%defaultVAR02 = root
UserName = char(handles.Settings.VariableValues{CurrentModuleNum,2});

%textVAR03 = Enter Password?
%defaultVAR03 = omero
Password = char(handles.Settings.VariableValues{CurrentModuleNum,3});

%pathnametextVAR04 = Enter the Hostname?
%defaultVAR04 = localhost
Hostname = char(handles.Settings.VariableValues{CurrentModuleNum,4});

%textVAR05 = What do you want to call these images within CellProfiler?
%defaultVAR05 = OrigBlue
%infotypeVAR05 = imagegroup indep
ImageName{1} = char(handles.Settings.VariableValues{CurrentModuleNum,5});

%textVAR06 = What channel position are these in the group?
%defaultVAR06 = Do not use
TextToFind{1} = char(handles.Settings.VariableValues{CurrentModuleNum,6});

%textVAR07 = What do you want to call these images within CellProfiler?
%defaultVAR07 = Do not use
%infotypeVAR07 = imagegroup indep
ImageName{2} = char(handles.Settings.VariableValues{CurrentModuleNum,7});

%textVAR08 = What channel position are these in the group?
%defaultVAR08 = Do not use
TextToFind{2} = char(handles.Settings.VariableValues{CurrentModuleNum,8});

%textVAR09 = What do you want to call these images within CellProfiler?
%defaultVAR09 = Do not use
%infotypeVAR09 = imagegroup indep
ImageName{3} = char(handles.Settings.VariableValues{CurrentModuleNum,9});

%textVAR10 = What channel position are these in the group?
%defaultVAR10 = Do not use
TextToFind{3} = char(handles.Settings.VariableValues{CurrentModuleNum,10});

%textVAR11 = What do you want to call these images within CellProfiler?
%defaultVAR11 = Do not use
%infotypeVAR11 = imagegroup indep
ImageName{4} = char(handles.Settings.VariableValues{CurrentModuleNum,11});

%textVAR12 = What channel position are these in the group?
%defaultVAR12 = Do not use
TextToFind{4} = char(handles.Settings.VariableValues{CurrentModuleNum,12});

%%%VariableRevisionNumber = 2


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% PRELIMINARY CALCULATIONS %%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
drawnow

%%% Determines which cycle is being analyzed.
SetBeingAnalyzed = handles.Current.SetBeingAnalyzed;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% FIRST CYCLE FILE HANDLING %%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
drawnow
    
%%% TODO: check what the pathname should be!
Pathname=['dataset: ',DatasetID];

%%% CREATE OMERO GATEWAY. Note: DO NOT FORGET TO CLOSE IT!
client = omero.client(java.lang.String(Hostname), 4063)
session = client.createSession(UserName, Password)
omeroService = session.createGateway()
    
%%% Extracting the list of files to be analyzed occurs only the first time
%%% through this module.
if SetBeingAnalyzed == 1
    %iceConfigPath = strcat(Pathname,'/ice.config');
    %omeroService = createOmeroJavaService(Hostname,UserName, Password);
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    %%% Check that the Project, Dataset and images exist in Dataset. %
    %%% TODO:                                                        %
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    datasetAsNum = [str2num(DatasetID)];
    dataset = getDataset(omeroService, datasetAsNum, 1);
    list = omerojava.util.GatewayUtils.getImagesFromDataset(dataset);
    
    fileIds=[];
    cnt = 0;
    for i = 1:list.size()
        pixelsList = getPixelsFromImage(omeroService, list.get(i-1).getId.getValue);
        if(~isempty(pixelsList))
            for j = 0:pixelsList.size-1;
                pixels = pixelsList.get(j);
                %if(isempty(pixels.RELATEDTO))
                    cnt = cnt +1;
                    fileIds(cnt)= pixels.getId.getValue;
                %end;
            end
        end
    end
    %%% Checks whether any files have been found
    if isempty(fileIds)
        error(['Image processing was canceled in the ', ModuleName, ' module because there are no files in the chosen dataset.'])
    end
    imagesPerSet = numImages(ImageName);
    handles.Pipeline.('imagesPerSet') = imagesPerSet;
    NumberOfImageSets = 0;
    for i = 1:length(fileIds),
        pixels = getPixels(omeroService, fileIds(i));
        for z  = 0:pixels.getSizeZ.getValue-1,
            for t = 0:pixels.getSizeT.getValue-1,
                fieldName =  strcat('FileCnt',num2str(NumberOfImageSets+1));
                handles.Pipeline.(fieldName) = strcat('FileId',num2str(fileIds(i)),'z',num2str(z),'t',num2str(t));
                NumberOfImageSets = NumberOfImageSets + 1;
            end
        end
    end
    handles.Current.NumberOfImageSets = NumberOfImageSets;

    clear fileIds
    clear pixels
    clear fieldName
%    if (~omeroService.isClosed())
%        omeroService.close();
%    end
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% LOADING IMAGES EACH TIME %%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
drawnow

handles.Pipeline.('Hostname') = Hostname;
handles.Pipeline.('UserName') = UserName;
handles.Pipeline.('Password') = Password;
%omeroService = createOmeroJavaService(Hostname,UserName, Password);

for n = 1:handles.Pipeline.imagesPerSet
        %%% This try/catch will catch any problems in the load images module.
        try
            fieldname = strcat('FileCnt', num2str(SetBeingAnalyzed));
            currentFileDetails = handles.Pipeline.(fieldname);
            [pixelsId, z, t] = parseFileDetails(currentFileDetails);
            [LoadedImage, handles] = CPOMEROimread(omeroService, currentFileDetails, TextToFind{n}, handles);
            if (max(LoadedImage(:)) <= .0625) && (handles.Current.SetBeingAnalyzed == 1)
                A = strmatch('RescaleIntensity', handles.Settings.ModuleNames);
                if length(A) < length(ImageName)
                    CPwarndlg(['Warning: the images loaded by ', ModuleName, ' are very dim (they are using 1/16th or less of the dynamic range of the image file format). This often happens when a 12-bit camera saves in 16-bit image format. If this is the case, use the Rescale Intensity module in "Enter max and min" mode to rescale the images using the values 0, 0.0625, 0, 1.'],'Outside 0-1 Range','replace');
                end
            end

            fieldname = strcat('Filename', ImageName{n});
            handles.Pipeline.(ImageName{n}) = LoadedImage;
            pixels = getPixels(omeroService, pixelsId);
            imageId = pixels.getImage.getId.getValue;
            [path, fname, ext, v] = fileparts(char(omeroService.getImage(imageId).getName.getValue));

            fName = strcat(fname, 'z', num2str(z), 't', num2str(t),'c',TextToFind{n},ext);
            handles.Pipeline.(fieldname)(SetBeingAnalyzed) = {fName};
        catch ErrorMessage = lasterr;
            ErrorNumber = {'first','second','third','fourth'};
            error(['Image processing was canceled in the ', ModuleName, ' module because an error occurred when trying to load the ', ErrorNumber{n}, ' set of images. Please check the settings. A common problem is that there are non-image files in the directory you are trying to analyze. Matlab says the problem is: ', ErrorMessage])
        end % Goes with: catch
    
        % Create a cell array with the filenames
       FileNames{n} = {char(omeroService.getImage(imageId).getName.getValue), ':', num2str(z), ':', num2str(t)};
end
%if (~omeroService.isClosed())
    omeroService.close();
%end
client.closeSession()
%%%%%%%%%%%%%%%%%%%%%%%
%%% DISPLAY RESULTS %%%
%%%%%%%%%%%%%%%%%%%%%%%
drawnow

ThisModuleFigureNumber = handles.Current.(['FigureNumberForModule',CurrentModule]);
if any(findobj == ThisModuleFigureNumber);
    if handles.Current.SetBeingAnalyzed == handles.Current.StartingImageSet
        CPresizefigure('','NarrowText',ThisModuleFigureNumber)
    end
    for n = 1:numImages(ImageName)
        %%% Activates the appropriate figure window.
        currentfig = CPfigure(handles,'Text',ThisModuleFigureNumber);
        if iscell(ImageName)
            TextString = [ImageName{n},': ', FileNames{n}];
        else
            TextString = [ImageName,': ',FileNames];
        end
        uicontrol(currentfig,'style','text','units','normalized','fontsize',handles.Preferences.FontSize,'HorizontalAlignment','left','string',TextString,'position',[.05 .85-(n-1)*.15 .95 .1],'BackgroundColor',[.7 .7 .9])
    end
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% SAVE DATA TO HANDLES %%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% NOTE: The structure for filenames and pathnames will be a cell array of cell arrays

%%% First, fix feature names and the pathname
PathNames = cell(1,numImages(ImageName));
FileNamesText = cell(1,numImages(ImageName));
PathNamesText = cell(1,numImages(ImageName));
for n = 1:numImages(ImageName)
    PathNames{n} = Pathname;
    FileNamesText{n} = [ImageName{n}];
    PathNamesText{n} = [ImageName{n}];
end

%%% Since there may be several load/save modules in the pipeline which all
%%% write to the handles.Measurements.Image.FileName field, we store
%%% filenames in an "appending" style. Here we check if any of the modules
%%% above the current module in the pipeline has written to
%%% handles.Measurements.Image.Filenames. Then we should append the current
%%% filenames and path names to the already written ones. If this is the
%%% first module to put anything into the handles.Measurements.Image
%%% structure, then this section is skipped and the FileNamesText fields
%%% are created with their initial entry coming from this module.

if  isfield(handles,'Measurements') && isfield(handles.Measurements,'Image') &&...
        isfield(handles.Measurements.Image,'FileNames') && length(handles.Measurements.Image.FileNames) == SetBeingAnalyzed
    % Get existing file/path names. Returns a cell array of names
    ExistingFileNamesText = handles.Measurements.Image.FileNamesText;
    ExistingFileNames     = handles.Measurements.Image.FileNames{SetBeingAnalyzed};
    ExistingPathNamesText = handles.Measurements.Image.PathNamesText;
    ExistingPathNames     = handles.Measurements.Image.PathNames{SetBeingAnalyzed};
    % Append current file names to existing file names
    FileNamesText = cat(2,ExistingFileNamesText,FileNamesText);
    FileNames     = cat(2,ExistingFileNames,FileNames);
    PathNamesText = cat(2,ExistingPathNamesText,PathNamesText);
    PathNames     = cat(2,ExistingPathNames,PathNames);
end

%%% Write to the handles.Measurements.Image structure
handles.Measurements.Image.FileNamesText                   = FileNamesText;
%handles.Measurements.Image.FileNames(SetBeingAnalyzed)         = {FileNames};
handles.Measurements.Image.PathNamesText                   = PathNamesText;
%handles.Measurements.Image.PathNames(SetBeingAnalyzed)         = {PathNames};

for n = 1:handles.Pipeline.imagesPerSet
    FileNames{n} = strcat(FileNames{n}{:});
end

handles.Measurements.Image.FileNames(SetBeingAnalyzed) = FileNames;
handles.Measurements.Image.PathNames(SetBeingAnalyzed) = PathNames;

%%%CPwritemeasurements.m, which is used by the ExportToExcel module,
%%%requires that the first field of handles.Measure holds as many elements
%%%as there are images.
if handles.Current.SetBeingAnalyzed == handles.Current.NumberOfImageSets
    fields = fieldnames(handles.Measurements.Image);
    if strcmp(fields{1}, 'FileNamesText'), %%% ONLY modify when we're sure FileNamesText is the first field
        handles.Measurements.Image.FileNamesText=handles.Measurements.Image.FileNames;
    end
end
